Unicode true
ManifestDPIAware true
SetCompressor lzma
!addplugindir /x86-unicode "plugins/x86-unicode"
!include "MUI2.nsh"

!define APPNAME "ApkStart"
!define APPVER "0.5.8"
!define UNKEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\ApkStart"

Name "${APPNAME} ${APPVER} (Español)"
OutFile "ApkStart-0.5.8-Setup-es.exe"
InstallDir "$LOCALAPPDATA\Programs\ApkStart"
InstallDirRegKey HKCU "${UNKEY}" "InstallLocation"
RequestExecutionLevel user
BrandingText "ApkStart ${APPVER} · traducción al español"

VIProductVersion "0.5.8.0"
VIAddVersionKey /LANG=1034 "ProductName" "ApkStart"
VIAddVersionKey /LANG=1034 "FileDescription" "Instalador de ApkStart (español)"
VIAddVersionKey /LANG=1034 "FileVersion" "0.5.8"
VIAddVersionKey /LANG=1034 "ProductVersion" "0.5.8"
VIAddVersionKey /LANG=1034 "LegalCopyright" "MIT · RoutineMiku"

!define MUI_ICON "payload/app/src/ui/assets/appicon.ico"
!define MUI_UNICON "payload/app/src/ui/assets/appicon.ico"
!define MUI_ABORTWARNING
!define MUI_FINISHPAGE_RUN "$INSTDIR\ApkStart.exe"
!define MUI_FINISHPAGE_RUN_TEXT "Abrir ApkStart"

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_LANGUAGE "Spanish"

Section "ApkStart" SecMain
  ; cerrar ApkStart si está abierto (bloquearía los archivos)
  nsExec::Exec 'taskkill /F /IM ApkStart.exe'
  Sleep 1500

  SetOutPath "$INSTDIR"
  DetailPrint "Extrayendo ApkStart y el entorno Android (puede tardar unos minutos)…"
  InitPluginsDir
  SetCompress off
  File "/oname=$PLUGINSDIR\app-64.7z" "payload/app-64.7z"
  SetCompress auto
  Nsis7z::ExtractWithDetails "$PLUGINSDIR\app-64.7z" "Extrayendo %s…"
  Delete "$PLUGINSDIR\app-64.7z"

  SetOutPath "$INSTDIR\resources\runtime\emulator\resources"
  File "payload/default.mp4"

  ; interfaz traducida al español encima de la original
  DetailPrint "Aplicando la traducción al español…"
  RMDir /r "$INSTDIR\resources\app\src"
  SetOutPath "$INSTDIR\resources\app"
  File /r "payload/app/*.*"

  SetOutPath "$INSTDIR"
  WriteUninstaller "$INSTDIR\Uninstall ApkStart.exe"

  CreateShortCut "$DESKTOP\ApkStart.lnk" "$INSTDIR\ApkStart.exe"
  CreateShortCut "$SMPROGRAMS\ApkStart.lnk" "$INSTDIR\ApkStart.exe"

  WriteRegStr HKCU "${UNKEY}" "DisplayName" "ApkStart ${APPVER}"
  WriteRegStr HKCU "${UNKEY}" "DisplayVersion" "${APPVER}"
  WriteRegStr HKCU "${UNKEY}" "Publisher" "RoutineMiku"
  WriteRegStr HKCU "${UNKEY}" "DisplayIcon" "$INSTDIR\ApkStart.exe,0"
  WriteRegStr HKCU "${UNKEY}" "InstallLocation" "$INSTDIR"
  WriteRegStr HKCU "${UNKEY}" "UninstallString" '"$INSTDIR\Uninstall ApkStart.exe"'
  WriteRegStr HKCU "${UNKEY}" "QuietUninstallString" '"$INSTDIR\Uninstall ApkStart.exe" /S'
  WriteRegDWORD HKCU "${UNKEY}" "NoModify" 1
  WriteRegDWORD HKCU "${UNKEY}" "NoRepair" 1
SectionEnd

Section "Uninstall"
  nsExec::Exec 'taskkill /F /IM ApkStart.exe'
  Sleep 1500
  Delete "$DESKTOP\ApkStart.lnk"
  Delete "$SMPROGRAMS\ApkStart.lnk"
  RMDir /r "$INSTDIR"
  DeleteRegKey HKCU "${UNKEY}"
  DeleteRegValue HKCU "Software\Microsoft\Windows\CurrentVersion\Run" "ApkStart"
SectionEnd
