# jna-wintun

jna-wintun is a wrapper for [wintun](https://www.wintun.net), which is a handy library
for creating and operating tun devices on Windows. This wrapper implemented an OOP
way to interact with your tun adapter and wintun session, using [Java Native Access (JNA)](https://github.com/java-native-access/jna).

This lib is written and tested in Kotlin. Although it should be compatible with Java,
I do use some special type from Kotlin, like [Unsigned integer types](https://kotlinlang.org/docs/unsigned-integer-types.html)
for those `ULONG` things in Microsoft C. So carefully evaluate before use this lib in
production environment.

## Goals

The main goal of this project is to offer a way to operate TUN device in Java, on Windows.

Unlike Linux, Windows has it own way to handle network adapters and ip addresses.
And it's messy. This project aims to deliver a simple and OOP-style methodology.

## How to use

You may use the jitpack for maven distribution, and you can also build yourself.

**Note: This repo does NOT contain the ddl from wintun, you have to download it
and put it somewhere. See jna's document to find out how to load your ddl.**

**Note: You have to run your code as admin, otherwise all wintun related operation
will be failed because of err 5 "Access denied".**

To use it, here is a quick glance of code:

```kotlin
fun main() {
    println("Current wintun version: ${WintunLib.INSTANCE.WintunGetRunningDriverVersion()}")
    val guid = Guid.GUID.newGuid().toGuidString()
    val adapter = WintunAdapter("Wintun Demo Adapter", "Wintun", guid)
    // Ring size: 8MB
    val session = adapter.newSession(0x800000)
    adapter.associateIp( // adapter ip: 0020::100/7
        AdapterIPAddress(ip = Inet6Address.getByName("0020::100"), prefixLength = 7u)
    )

    // reading
    while (true) {
        // try read, timeout 6s
        val result = session.readPacket(6000u)
        if (result != null) { // if not timeout
            // handle your raw ip packet here
        }
    }
    // writing
    session.writePacket(byteArrayOfYourIpPacket)
    // closing
    session.close()
    adapter.close()
}
```

By using [pcap4j](https://github.com/kaitoy/pcap4j), you can parse the raw IP packets
into some objects, like IP packets, ICMP packets. TCP packets, etc. And you can even
construct your own IP packets and send them back. Here in the [example](src/test/kotlin/info/skyblond/jna/WintunPingDemo.kt),
I implement a tun adapter that response to ICMP ECHO/PING.

## Behind the wrap

There are mainly two parts. One for wintun itself, one for iphlpapi.

Wintun is used to create and close adapters, create, operate and close sessions.
To manage your adapter's ip, you have to talk with iphlpapi to mess around with
your system's unicast ip MIB table, which is super annoying, considering you have
to map all structures from C to Java, suffering from the ugly MS style (but MS
did a good job on documenting their things).

For normal usage, `info.skyblond.jna.wintun.WintunAdapter` should be fine. But do
notice this is not thread safe. By initializing a `WintunAdapter` object, depends
on how many parameter you have, you can a) create a tun adapter by supplying the name,
type and guid, or b) open a existing one by a given name.

Then you can use `adapter.newSession(capacity)` to create session with certain ring
size. The session is used to talking to and from your adapter, like the `/dev/net/tun0`.
This will give you a `WintunSession` object, you can `readPacket` from it, or 
`writePacket` to it. And finally `close()` them after you finished.

So far, all things are implemented by invoking wintun. The `readPacket` has a slight
problem: sometimes there are nothing to read. To solve this, we can ask a handle
from wintun for a certain session, as the read event. And we can ask Windows' kernel
to suspend us until that read event happened (aka, waiting).

But how to configure ip address and MTU? You can do it by using `adapter.associateIp`
and `adapter.setMTU`. This is done by changing the `MIB_UNICASTIPADDRESS_ROW` and
`MIB_IPINTERFACE_ROW`, you can head to [netioapi.h](https://learn.microsoft.com/en-us/windows/win32/api/netioapi/)
for more info.

## Contributing and License

You're welcomed to contribute to this project. By contribute, I mean testing on
different versions of Windows, fixing bugs, implementer new features, and so on.

But as usual, this is one of my many side project. I won't maintain it forever,
and I can't promise you it's bug free (although there are less than 1K loc).

Also, the license is AGPL v3, it's not the best license for open source project,
but it's good for open source. Maybe LGPL is a good choice? I don't know, I'm not
a layer ¯\_(ツ)_/¯
