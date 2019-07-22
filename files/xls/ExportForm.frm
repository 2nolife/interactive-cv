VERSION 5.00
Begin {C62A69F0-16DC-11CE-9E98-00AA00574A4F} ExportForm 
   Caption         =   "Exporting"
   ClientHeight    =   1200
   ClientLeft      =   -1360
   ClientTop       =   -7820
   ClientWidth     =   7060
   OleObjectBlob   =   "ExportForm.frx":0000
   StartUpPosition =   1  'CenterOwner
End
Attribute VB_Name = "ExportForm"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Private Sub UserForm_Activate()

    ExportForm.ProgressLabel.Caption = ""
    ExportModule.ExportAsNTriples

End Sub
