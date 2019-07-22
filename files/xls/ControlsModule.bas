Attribute VB_Name = "ControlsModule"
Public DropdownNames(1 To 11) As String

Sub Init()

    Sheet4.Select
    
    ' dropdown names
    DropdownNames(1) = "Terms"
    DropdownNames(2) = "Hobby"
    DropdownNames(3) = "About"
    DropdownNames(4) = "Available"
    DropdownNames(5) = "Contact"
    DropdownNames(6) = "Education"
    DropdownNames(7) = "Reference"
    DropdownNames(8) = "Skill"
    DropdownNames(9) = "Contribution"
    DropdownNames(10) = "Company"
    DropdownNames(11) = "Assignment"

End Sub

Private Function FindCellByValue(value As String, colls As String) As Range
    
    Dim cell As Range

    Columns(colls).Select
    Set cell = Selection.Find(What:=value, After:=ActiveCell, LookIn:=xlValues)
    
    Set FindCellByValue = cell
    
End Function

Private Function FindCellByComment(value As String, colls As String) As Range
    
    Dim cell As Range

    Columns(colls).Select
    Set cell = Selection.Find(What:=value, After:=ActiveCell, LookIn:=xlComments)
    
    Set FindCellByComment = cell
    
End Function

Private Function GroupRangeByName(name As String) As Range
    ' line 1 - group begin token, line 2 - header, last line - group end token

    Dim cell As Range, beginCell As Range, endCell As Range

    Set beginCell = FindCellByValue(name + " BEGIN", "A:A")
    Set endCell = FindCellByValue(name + " END", "A:A")
    Set cell = Range(beginCell.Offset(2, 0), endCell.Offset(-1, 0))
    
    Set GroupRangeByName = cell
    
End Function

Sub CopyDropdown(groupName As String, sheetName As String, cellAddress As String)
    
    Sheet4.Select
    
    Dim beginCell As Range, controlCell As Range
    Set beginCell = FindCellByValue(groupName + " BEGIN", "A:A")
    Set controlCell = beginCell.Offset(0, 1)

    controlCell.Select
    Selection.Copy
    
    Sheets(sheetName).Select
    Range(cellAddress).Select
    
    Dim cellValue As String
    cellValue = ActiveCell.value
    ActiveSheet.Paste
    ActiveCell.value = cellValue

    Dim validationFormula As String
    validationFormula = Range(cellAddress).Validation.Formula1
    validationFormula = Right(validationFormula, Len(validationFormula) - 1)
    validationFormula = "=Controls!" + validationFormula

    Range(cellAddress).Validation.Modify Formula1:=validationFormula
    
End Sub

Sub ExposeDropdownControls(sheetName As String, groupName As String)
    ' convert cell into dropdown and preverve cell value as selected dropdown item
    ' cell on any sheet will be replaced by a copy from controls sheet
    
    Sheets(sheetName).Select

    Dim cell As Range, lastCellAddress As String
    
    Columns("A:C").Select
    lastCellAddress = ActiveCell.Address
    
    Do While True
        Set cell = Selection.Find(What:=groupName + "Dropdown", After:=ActiveCell, LookIn:=xlComments)
        
        If cell Is Nothing Then
            Exit Do
        ElseIf lastCellAddress = cell.Address Then
            Exit Do
        Else
            CopyDropdown groupName, sheetName, cell.Address
            lastCellAddress = cell.Address
        End If
    Loop
    
End Sub

Function GetUriByValue(value As String) As String
    
    Sheet4.Select

    Dim cell As Range
    Set cell = FindCellByValue(value, "B:B")

    GetUriByValue = cell.Offset(0, -1).value
    
End Function

Function GetUriTypeByUri(uri As String) As String
    
    Sheet4.Select

    Dim cell As Range, uriType As String
    Set cell = FindCellByValue(uri, "A:A")
    
    GetUriTypeByUri = cell.Offset(0, 2).value
    
End Function

