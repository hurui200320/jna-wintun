package info.skyblond.jna

import com.sun.jna.platform.win32.Guid
import info.skyblond.jna.wintun.*
import org.pcap4j.packet.*
import org.pcap4j.packet.namednumber.IcmpV6Code
import org.pcap4j.packet.namednumber.IcmpV6Type
import org.pcap4j.packet.namednumber.IpNumber
import org.pcap4j.packet.namednumber.IpVersion
import java.io.EOFException
import java.net.Inet6Address
import kotlin.concurrent.thread
import kotlin.experimental.and
import kotlin.random.Random

/**
 * In this demo, we will create a tun device,
 * assigned ipv6 address with 0020::100/7.
 * This demo will handle any ping to this subnet by faking
 * ICMPv6 ECHO reply.
 *
 * Try ping 0020::300 while running this demo.
 *
 * Note: You need run this as admin to create/operate with tun devices.
 * */
object WintunPingDemo {

    @Volatile
    private var ip = Inet6Address.getByName("0020::100")

    private fun handlePacket(session: WintunSession, packet: ByteArray) {
        val isV6 = packet[0].and(0xf0.toByte()) == 0x60.toByte()
        println(
            "Get IPv${if (isV6) "6" else "4"} packet from OS\n" +
                    "\tSize: ${packet.size} bytes\n"
        )

        if (isV6) {
            val v6Packet = IpV6Packet.newPacket(packet, 0, packet.size)
            println(v6Packet)
            (v6Packet.payload as? IcmpV6CommonPacket)?.let { icmpV6Common ->
                (icmpV6Common.payload as? IcmpV6EchoRequestPacket)?.let { request ->
                    val reply = IpV6Packet.Builder()
                        .version(IpVersion.IPV6)
                        .trafficClass(IpV6SimpleTrafficClass.newInstance(0x00))
                        .flowLabel(IpV6SimpleFlowLabel.newInstance(0))
                        .srcAddr(v6Packet.header.dstAddr)
                        .dstAddr(v6Packet.header.srcAddr)
                        .nextHeader(IpNumber.ICMPV6)
                        .hopLimit(127)
                        .correctLengthAtBuild(true)
                        .payloadBuilder(
                            IcmpV6CommonPacket.Builder()
                                .srcAddr(v6Packet.header.dstAddr)
                                .dstAddr(v6Packet.header.srcAddr)
                                .type(IcmpV6Type.ECHO_REPLY)
                                .code(IcmpV6Code.NO_CODE)
                                .correctChecksumAtBuild(true)
                                .payloadBuilder(
                                    IcmpV6EchoReplyPacket.Builder()
                                        .identifier(request.header.identifier)
                                        .sequenceNumber(request.header.sequenceNumber)
                                        .payloadBuilder(request.payload.builder)
                                )
                        )
                        .build()
                    println("Reply: \n$reply")
                    session.writePacket(reply.rawData)
                }
            }
        } else {
            val v4Packet = IpV4Packet.newPacket(packet, 0, packet.size)
            println(v4Packet)
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println("Current wintun version: ${WintunLib.INSTANCE.WintunGetRunningDriverVersion()}")
        val guid = Guid.GUID.newGuid().toGuidString()
        val adapter = WintunAdapter("Wintun Demo Adapter", "Wintun", guid)
        // Ring size: 8MB
        val session = adapter.newSession(0x800000)

        thread {
            while (true) {
                try { // clean up old ip
                    adapter.dissociateIp(ip)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }

                val random = Random.nextInt(5, 200)
                ip = Inet6Address.getByName("0020::$random")
                println("Set ip to: $ip")
                adapter.associateIp(
                    AdapterIPAddress(ip = ip, prefixLength = 7u)
                )

                Thread.sleep(1000 * 60)
            }
        }

        val t = thread {
            try {
                while (true) {
                    val result = session.readPacket()
                    if (result != null) thread { handlePacket(session, result) }
                }
            } catch (e: EOFException) {
                e.printStackTrace()
            } catch (e: NativeException) {
                e.printStackTrace()
            }
        }

        while (t.isAlive) {
            Thread.sleep(1000)
        }
        println("Closing!")
        session.close()
        adapter.close()
    }
}
