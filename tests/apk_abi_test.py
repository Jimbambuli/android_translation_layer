#!/usr/bin/python3
# apk_abi_test.py TEST_BINARY: the ABI choice for APKs with native code for several ABIs, the device ABI list being
# x86_64 first and arm64-v8a (the native bridge) second, as ATL lists them
import os, subprocess, sys, tempfile, zipfile

test = sys.argv[1]
tmp = tempfile.mkdtemp()

def apk(name, files):
    path = os.path.join(tmp, name)
    with zipfile.ZipFile(path, "w") as z:
        z.writestr("AndroidManifest.xml", b"x")
        for f in files:
            z.writestr(f, b"x")
    return path

def choose(paths, abis=("x86_64", "arm64-v8a")):
    return int(subprocess.run([test, ":".join(paths), *abis], capture_output=True, text=True, check=True).stdout)

cases = [
    # (a) both ABIs: x86_64, the bridge is not needed
    ("both", [apk("both.apk", ["lib/x86_64/libfoo.so", "lib/arm64-v8a/libfoo.so"])], 0),
    # (b) split APKs: base without code, an x86_64 and an arm64-v8a part
    ("splits", [apk("base.apk", []), apk("x.apk", ["lib/x86_64/libfoo.so"]), apk("a.apk", ["lib/arm64-v8a/libfoo.so"])], 0),
    ("splits, arm part first", [apk("base2.apk", []), apk("a2.apk", ["lib/arm64-v8a/libfoo.so"]), apk("x2.apk", ["lib/x86_64/libfoo.so"])], 0),
    # (c) only arm64-v8a: the native bridge
    ("arm64 only", [apk("arm.apk", ["lib/arm64-v8a/libfoo.so", "lib/armeabi-v7a/libfoo.so"])], 1),
    # (d) only 32-bit x86 (and 32-bit ARM): no ABI of the list
    ("x86 only", [apk("x86.apk", ["lib/x86/libfoo.so", "lib/armeabi-v7a/libfoo.so"])], -1),
    # (e) fewer x86_64 libraries than arm64-v8a ones: the list order decides, as in AOSP
    ("x86_64 fewer", [apk("fewer.apk", ["lib/x86_64/liba.so"] + ["lib/arm64-v8a/lib%d.so" % i for i in range(5)])], 0),
    # no native code; files that are no library entries
    ("none", [apk("none.apk", ["assets/lib/x86_64/libfoo.so", "lib/x86_64/", "lib/x86_64/sub/libfoo.so"])], -1),
]
failed = 0
for name, paths, expected in cases:
    got = choose(paths)
    print(f"{name}: {got} (expected {expected})")
    failed += got != expected
# without the bridge only x86_64 counts
got = choose([os.path.join(tmp, "arm.apk")], ("x86_64",))
print(f"arm64 only, no bridge: {got} (expected -1)")
failed += got != -1
got = choose([os.path.join(tmp, "missing.apk")])
print(f"unreadable: {got} (expected -2)")
failed += got != -2
sys.exit(1 if failed else 0)
