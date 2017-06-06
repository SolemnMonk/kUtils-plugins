rem I have a folder called "bnd" at the same level as my Git repos that I store my bundles in.
rem This script copies my generated bundles to a kUtils folder within that.
mkdir ..\bnd\kUtils
for /D %%a in (kUtils.*) do copy "%%a\generated\*.jar" "..\bnd\kUtils"
