Denial of Service in Elliptic Curve Parsing/Processing
======================================================

Overview
--------
When parsing an ASN.1 ECParameters structure OpenSSL enters
an infinite loop if the curve specified is over a specially
malformed binary polynomial field.

This can be used to perform denial of service against any
system which processes public keys, certificate requests or
certificates.  This includes TLS clients and TLS servers with
client authentication enabled.

Workarounds
-----------
Compile openssl with `--no-ec2m`.  This disables all support
for elliptic curves over binary polynomial fields.

Patches
-------
Three patches are included:

- `tests.patch`: this is a large patch which only improves testing
  in this area. It includes a regression test for this issue and
  a selection of other targeted tests.

- `fix1.patch`: this patch fixes the issue by reinstating the
  existing fixed code and deleting the broken optimised version.
  This is a minimal change, and means the code is left clean
  and as readable as possible.

- `fix2.patch`: this patch fixes the issue by correcting the
  optimised version, and deleting the commented out version.
  This means the code is left fast but opaque.

PoC
---
`original-cert.der` is a basic X509 certificate with a public
key on the ANSI X9.62 `c2pnb208w1` curve.  The choice of curve
is not important, except it must be a binary curve, and must
be explicitly specified.

`broken-cert.der`: the same file, with edits to trigger the bug.
This is not a unique set of edits; a fuzzer will find others.

The requirements are that the polynomial is not irreducible,
and that the base point is not uncompressed.  The base point
decompression happens during parsing, and involves a call
to the broken function `BN_GF2m_mod_inv` via `EC_POINT_oct2point`.

`dumb-server.py` is a trivial TLS server.  Its sole purpose
is to accept a `ClientHello`, and reply with a `ServerHello` and
`Certificate` containing the malformed certificate.  Get this from
https://github.com/ctz/tls-hacking

First, start the server:

    $ python3 dumb-server.py broken-cert.der
    listening on localhost 9999

Reproduce with Debian jessie `openssl s_client`:

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

Reproduce with Debian jessie `curl`:

    $ gdb -ex run --args curl https://localhost:9999
    <snip>
    Starting program: /usr/bin/curl https://localhost:9999
    [Thread debugging using libthread_db enabled]
    Using host libthread_db library "/lib/x86_64-linux-gnu/libthread_db.so.1".
    [New Thread 0x7ffff2770700 (LWP 5863)]
    [Thread 0x7ffff2770700 (LWP 5863) exited]
    <hang>
    ^C
    Program received signal SIGINT, Interrupt.
    0x00007ffff699819c in BN_GF2m_mod_inv () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    (gdb) bt
    #0  0x00007ffff699819c in BN_GF2m_mod_inv () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #1  0x00007ffff69984f6 in BN_GF2m_mod_div () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #2  0x00007ffff69b7488 in ?? () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #3  0x00007ffff69a153c in ?? () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #4  0x00007ffff69a271e in d2i_ECPKParameters () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #5  0x00007ffff69a2eaa in d2i_ECParameters () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #6  0x00007ffff69a6338 in ?? () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #7  0x00007ffff69a667c in ?? () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #8  0x00007ffff69f1ac8 in X509_PUBKEY_get () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #9  0x00007ffff6a0da02 in X509_get_pubkey_parameters () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #10 0x00007ffff6a0dc0a in X509_verify_cert () from /lib/x86_64-linux-gnu/libcrypto.so.1.0.0
    #11 0x00007ffff6d059c8 in ?? () from /lib/x86_64-linux-gnu/libssl.so.1.0.0
    #12 0x00007ffff6ce3d8b in ?? () from /lib/x86_64-linux-gnu/libssl.so.1.0.0
    #13 0x00007ffff6ce8002 in ?? () from /lib/x86_64-linux-gnu/libssl.so.1.0.0
    #14 0x00007ffff7bc0092 in ?? () from /usr/lib/x86_64-linux-gnu/libcurl.so.4
    #15 0x00007ffff7bc1d90 in ?? () from /usr/lib/x86_64-linux-gnu/libcurl.so.4
    #16 0x00007ffff7b82aee in ?? () from /usr/lib/x86_64-linux-gnu/libcurl.so.4
    #17 0x00007ffff7ba54b1 in ?? () from /usr/lib/x86_64-linux-gnu/libcurl.so.4
    #18 0x00007ffff7ba6101 in curl_multi_perform () from /usr/lib/x86_64-linux-gnu/libcurl.so.4
    #19 0x00007ffff7b9d733 in curl_easy_perform () from /usr/lib/x86_64-linux-gnu/libcurl.so.4
    #20 0x000000000040a45d in ?? ()
    #21 0x000000000040237e in ?? ()
    #22 0x00007ffff7397ec5 in __libc_start_main (main=0x402300, argc=2, argv=0x7fffffffe128, init=<optimised out>, 
        fini=<optimised out>, rtld_fini=<optimised out>, stack_end=0x7fffffffe118) at libc-start.c:287
    #23 0x00000000004023da in ?? ()

Root cause
----------
The original bug was introduced in 2002 with the first support
for elliptic curves over binary polynomial fields[1].  This was
reported and fixed in June 2011 (OpenSSL bug 2540)[2].

Shortly before, in May 2011, the code which would later contain
the fix was commented-out and replaced with an optimised version[3].

This means that the application of the patch in [2] went into
commented-out code, and had no effect.

The author posits that the root cause is checking in commented-out
code and lack of regression testing.

[1]: https://github.com/openssl/openssl/commit/1dc920c8de5b7109727a21163843feecdf06a8cf
[2]: https://github.com/openssl/openssl/commit/8038e7e44c6060398f0793e3e16db0ad1ee95b9d
[3]: https://github.com/openssl/openssl/commit/034688ec4d0e3d350dc0ee9602552f92e8889fc0

Fork status
-----------
- BoringSSL has deleted this code.
- LibreSSL has the affected code and is thought to be vulnerable (untested).

Author
------
Joseph Birr-Pixton <jpixton@gmail.com>

