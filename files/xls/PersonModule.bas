Attribute VB_Name = "PersonModule"
Sub Init()

    ' expose dropdowns
    ' Dim element As Variant
    ' For Each element In ControlsModule.DropdownNames
    '    ControlsModule.ExposeDropdownControls "Person", CStr(element)
    ' Next element
    
End Sub

Sub OnCellDoubleClick(cell As Range)

    If cell.comment Is Nothing Then Exit Sub

    Select Case cell.comment.text
        Case "NewLine:About"
            AboutSectionNewLine
        Case "NewLine:Hobby"
            HobbySectionNewLine
        Case "NewLine:Available"
            AvailableSectionNewLine
        Case "NewLine:Contact"
            ContactSectionNewLine
        Case "NewLine:Education"
            EducationSectionNewLine
        Case "NewLine:Reference"
            ReferenceSectionNewLine
        Case "NewLine:Skill"
            SkillSectionNewLine
        Case "NewLine:Contribution"
            ContributionSectionNewLine
        Case Else
            'do nothing
    End Select

End Sub

Private Sub InsertSectionNewLine(sectionName As String, nodeValue As String, termValue As String, dropdownGroupName As String)

    Columns("A:A").Select
    
    Dim cell As Range
    Set cell = Selection.Find(What:=sectionName + "Section BEGIN", After:=ActiveCell, LookIn:=xlComments)
    Set cell = Range(cell.Offset(1, 0), cell.Offset(1, 3))
    
    ' copy row
    cell.Rows(1).Select
    Selection.Insert Shift:=xlDown
    Selection.ClearFormats
    Selection.NumberFormat = "@"
        
    ' new row 1st cell
    Set cell = cell(1, 1).Offset(-1, 0)
    cell.value = nodeValue
    
    ' new row 2nd cell
    Set cell = cell.Offset(0, 1)
    ControlsModule.CopyDropdown dropdownGroupName, "Person", cell.Address
    cell.value = termValue
    
    ' new row 3rd cell
    cell.Offset(0, 1).Select

End Sub

Private Sub HobbySectionNewLine()

    InsertSectionNewLine "Hobby", "<hobby>", "hobby", "Hobby"

End Sub

Private Sub AboutSectionNewLine()

    InsertSectionNewLine "About", "<about>", "", "About"

End Sub

Private Sub AvailableSectionNewLine()

    InsertSectionNewLine "Available", "<available>", "", "Available"

End Sub

Private Sub ContactSectionNewLine()

    InsertSectionNewLine "Contact", "<contact>", "", "Contact"

End Sub

Private Sub EducationSectionNewLine()

    InsertSectionNewLine "Education", "<education>", "", "Education"

End Sub

Private Sub ReferenceSectionNewLine()

    InsertSectionNewLine "Reference", "<reference>", "referee", "Reference"

End Sub

Private Sub SkillSectionNewLine()

    InsertSectionNewLine "Skill", "<skill>", "knows", "Skill"

End Sub

Private Sub ContributionSectionNewLine()

    InsertSectionNewLine "Contribution", "<contribution>", "url", "Contribution"

End Sub

Function GetSectionTokens(sectionName As String) As Variant
    
    Sheet1.Select
    Columns("A:A").Select
    
    Dim beginCell As Range, endCell As Range, count As Integer
    Set beginCell = Selection.Find(What:=sectionName + "Section BEGIN", After:=ActiveCell, LookIn:=xlComments)
    Set endCell = Selection.Find(What:=sectionName + "Section END", After:=ActiveCell, LookIn:=xlComments)
    
    count = 0
    If Range(beginCell, endCell).count > 2 Then
        count = Range(beginCell.Offset(1, 0), endCell.Offset(-1, 0)).count
    End If
    
    Dim tokens() As Variant
    tokens = MainModule.CollectTripleTokens(beginCell, count)
    
    GetSectionTokens = tokens
    
End Function

