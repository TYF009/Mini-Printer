$ErrorActionPreference = 'Stop'
python (Join-Path $PSScriptRoot 'test-host.py')
if ($LASTEXITCODE -ne 0) { throw 'Host checks failed.' }
