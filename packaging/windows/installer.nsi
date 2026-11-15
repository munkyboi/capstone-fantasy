Unicode true
Name "Capstone Fantasy"
OutFile "../../build/windows/Capstone-Fantasy-Setup-x64.exe"
InstallDir "$LOCALAPPDATA\Programs\Capstone Fantasy"
RequestExecutionLevel user
SetCompressor /SOLID lzma

Page directory
Page instfiles
UninstPage uninstConfirm
UninstPage instfiles

Section "Install"
    SetOutPath "$INSTDIR"
    File "../../build/windows/Capstone-Fantasy/Capstone Fantasy.exe"
    SetOutPath "$INSTDIR\jre"
    File /r "../../build/windows/Capstone-Fantasy/jre/*"
    WriteUninstaller "$INSTDIR\Uninstall.exe"
    CreateDirectory "$SMPROGRAMS\Capstone Fantasy"
    CreateShortcut "$SMPROGRAMS\Capstone Fantasy\Capstone Fantasy.lnk" "$INSTDIR\Capstone Fantasy.exe" "" "$INSTDIR\Capstone Fantasy.exe"
    CreateShortcut "$SMPROGRAMS\Capstone Fantasy\Uninstall Capstone Fantasy.lnk" "$INSTDIR\Uninstall.exe"
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\CapstoneFantasy" "DisplayName" "Capstone Fantasy"
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\CapstoneFantasy" "UninstallString" '$\"$INSTDIR\Uninstall.exe$\"'
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\CapstoneFantasy" "DisplayIcon" "$INSTDIR\Capstone Fantasy.exe"
    WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\CapstoneFantasy" "NoModify" 1
    WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\CapstoneFantasy" "NoRepair" 1
SectionEnd

Section "Uninstall"
    Delete "$SMPROGRAMS\Capstone Fantasy\Capstone Fantasy.lnk"
    Delete "$SMPROGRAMS\Capstone Fantasy\Uninstall Capstone Fantasy.lnk"
    RMDir "$SMPROGRAMS\Capstone Fantasy"
    Delete "$INSTDIR\Capstone Fantasy.exe"
    Delete "$INSTDIR\Uninstall.exe"
    RMDir /r "$INSTDIR\jre"
    RMDir "$INSTDIR"
    DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\CapstoneFantasy"
SectionEnd
