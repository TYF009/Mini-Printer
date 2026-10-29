"""Run Android-independent Java checks on Windows, macOS and Linux."""
from pathlib import Path
import os, shutil, subprocess

root = Path(__file__).resolve().parent
java_home = os.environ.get("JAVA_HOME")
def tool(name):
    executable = name + (".exe" if os.name == "nt" else "")
    path = Path(java_home) / "bin" / executable if java_home else shutil.which(name)
    if not path:
        raise SystemExit("Install JDK 17 and set JAVA_HOME or PATH.")
    return str(path)
output = Path("build") / "host-tests"
(root / output).mkdir(parents=True, exist_ok=True)
source = Path("app/src/main/java/studio/foli/android")
subprocess.run([tool("javac"), "-encoding", "UTF-8", "-d", str(output),
                str(source / "PrinterProtocol.java"), str(source / "BleOperation.java"), str(source / "PrintOutcome.java"),
                "tests/ProtocolTest.java", "tests/BleOperationTest.java", "tests/PrintOutcomeTest.java"], check=True, cwd=root)
for test in ("ProtocolTest", "BleOperationTest", "PrintOutcomeTest"):
    subprocess.run([tool("java"), "-cp", str(output), test], check=True, cwd=root)
