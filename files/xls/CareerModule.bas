Attribute VB_Name = "CareerModule"
Sub Init()

    
End Sub

Sub OnCellDoubleClick(cell As Range)

    If cell.comment Is Nothing Then Exit Sub

    Select Case cell.comment.text
        Case "NewSection:Company"
            CompanySectionNew
        Case "NewLine:Company"
            cell.Offset(0, -3).Select
            CompanySectionNewLine
        Case "NewSection:Assignment"
            cell.Offset(0, -3).Select
            AssignmentSectionNew
        Case "NewLine:Assignment"
            cell.Offset(0, -3).Select
            AssignmentSectionNewLine
        Case Else
            'do nothing
    End Select

End Sub

Private Sub CompanySectionNew()

    ' copy row x3
    Range("A2:D4").Select
    Selection.Insert Shift:=xlDown
    Selection.ClearFormats
    Range("A3:D4").Select
    Selection.Style = "Neutral"
    
    Range("A3").value = "Company ID"
    Range("A3").AddComment "CompanySection BEGIN"
    Range("A4").AddComment "CompanySection END"
    Range("D3").AddComment "NewLine:Company"
    Range("D4").AddComment "NewSection:Assignment"
    With Range("D3:D4")
        .value = "+"
        .HorizontalAlignment = xlCenter
    End With
    Range("B3").Select
    
End Sub

Private Sub CompanySectionNewLine()

    Dim cell As Range
    Set cell = ActiveCell
    
    If cell.comment Is Nothing Then
        MsgBox "Select company section begin cell"
    ElseIf cell.comment.text <> "CompanySection BEGIN" Then
        MsgBox "Select company section begin cell"
    Else
        
        Set cell = Range(cell.Offset(1, 0), cell.Offset(1, 3))
        
        ' copy row
        cell.Rows(1).Select
        Selection.Insert Shift:=xlDown
        Selection.ClearFormats
        Selection.NumberFormat = "@"
            
        ' new row 1st cell
        Set cell = cell(1, 1).Offset(-1, 0)
        cell.value = "<company>"
        
        ' new row 2nd cell
        Set cell = cell.Offset(0, 1)
        ControlsModule.CopyDropdown "Company", "Career", cell.Address
        cell.value = "name"
        
        ' new row 3rd cell
        cell.Offset(0, 1).Select
        
    End If

End Sub

Private Sub AssignmentSectionNew()

    Dim cell As Range
    Set cell = ActiveCell
    
    If cell.comment Is Nothing Then
        MsgBox "Select company section end cell"
    ElseIf cell.comment.text <> "CompanySection END" Then
        MsgBox "Select company section end cell"
    Else
        
        Set cell = Range(cell, cell.Offset(0, 3))
        
        ' copy row x2
        cell.Rows("1:2").Select
        Selection.Insert Shift:=xlDown
        Selection.ClearFormats
        Selection.Style = "Neutral"

        ' new row
        Set cell = Selection(1)
        cell.value = "Assignment"
        cell.AddComment "AssignmentSection BEGIN"
        cell.Offset(1, 0).AddComment "AssignmentSection END"
        With cell.Offset(0, 3)
            .AddComment "NewLine:Assignment"
            .value = "+"
            .HorizontalAlignment = xlCenter
        End With

        AssignmentSectionNewLine
        
    End If

End Sub

Private Sub AssignmentSectionNewLine()

    Dim cell As Range
    Set cell = ActiveCell
    
    If cell.comment Is Nothing Then
        MsgBox "Select assignment section begin cell"
    ElseIf cell.comment.text <> "AssignmentSection BEGIN" Then
        MsgBox "Select assignment section begin cell"
    Else
        
        Set cell = Range(cell.Offset(1, 0), cell.Offset(1, 3))
        
        ' copy row
        cell.Rows(1).Select
        Selection.Insert Shift:=xlDown
        Selection.ClearFormats
        Selection.NumberFormat = "@"
            
        ' new row 1st cell
        Set cell = cell(1, 1).Offset(-1, 0)
        cell.value = "<assignment>"
        
        ' new row 2nd cell
        Set cell = cell.Offset(0, 1)
        ControlsModule.CopyDropdown "Assignment", "Career", cell.Address
        cell.value = ""
        
        ' new row 3rd cell
        cell.Offset(0, 1).Select
        
    End If

End Sub

Private Function CountCellsWithComment(comment As String) As Integer
    
    Dim cell As Range, lastCellRowNumber As Integer, count As Integer
    
    Columns("A:A").Select
    lastCellRowNumber = ActiveCell.Row
    Set cell = ActiveCell
    count = 0
    
    Do While True
        Set cell = Selection.Find(What:=comment, After:=cell, LookIn:=xlComments)
        If cell Is Nothing Then
            Exit Do
        ElseIf lastCellRowNumber >= cell.Row Then
            Exit Do
        Else
            count = count + 1
            lastCellRowNumber = cell.Row
        End If
    Loop
    
    CountCellsWithComment = count
    
End Function

Function GetCompanyIds() As Variant
    
    Sheet2.Select
        
    ' count total elements
    Dim count As Integer
    count = CountCellsWithComment("CompanySection BEGIN")
    
    If count = 0 Then
        ' no companies found
    Else
        ' fill array
        Columns("A:A").Select
        Dim ids() As Variant, i As Integer, cell As Range
        ReDim ids(1 To count) As Variant
        Set cell = ActiveCell
        For i = 1 To count
            Set cell = Selection.Find(What:="CompanySection BEGIN", After:=cell, LookIn:=xlComments)
            ids(i) = cell.Offset(0, 1).value
        Next i
    End If
    
    GetCompanyIds = ids
    
End Function

Function GetAssignmentIds() As Variant
    
    Sheet2.Select
        
    ' count total elements
    Dim count As Integer
    count = CountCellsWithComment("AssignmentSection BEGIN")
    
    If count = 0 Then
        ' no assignments found
    Else
        ' fill array
        Columns("A:A").Select
        Dim ids() As Variant, i As Integer, cell As Range, Id As String
        ReDim ids(1 To count) As Variant
        Set cell = ActiveCell
        For i = 1 To count
            Set cell = Selection.Find(What:="AssignmentSection BEGIN", After:=cell, LookIn:=xlComments)
            Id = cell.Offset(0, 1).comment.text
            Id = Right(Id, Len(Id) - 13)
            Id = Left(Id, Len(Id) - 1)
            ids(i) = Id
        Next i
    End If
    
    GetAssignmentIds = ids
    
End Function

Private Function FindCompanyBeginCell(companyId As String) As Range

    ' find company begin cell (it must exist)
    Dim cell As Range, beginCell As Range
    
    Columns("A:A").Select
    Set cell = ActiveCell
    
    Do While True
        Set cell = Selection.Find(What:="CompanySection BEGIN", After:=cell, LookIn:=xlComments)
        If cell.Offset(0, 1).value = companyId Then
            Set beginCell = cell
            Exit Do
        End If
    Loop

    Set FindCompanyBeginCell = beginCell

End Function

Private Function FindAssignmentBeginCell(assignmentId As String) As Range

    ' find assignment begin cell (it must exist)
    Dim cell As Range
    
    Columns("B:B").Select
    Set cell = Selection.Find(What:="AssignmentId:" + assignmentId + "!", After:=ActiveCell, LookIn:=xlComments)
    Set cell = cell.Offset(0, -1)

    Set FindAssignmentBeginCell = cell

End Function

Function GetCompanyTokens(companyId As String) As Variant
    
    Sheet2.Select
        
    Dim cell As Range, beginCell As Range, count As Integer
    Set beginCell = FindCompanyBeginCell(companyId)
    Set cell = beginCell
    count = 0
    
    ' tokens come after the company begin cell and before any cell with comment, count total
    Do While True
        Set cell = cell.Offset(1, 0)
        If cell.comment Is Nothing Then
            count = count + 1
        Else
            Exit Do
        End If
    Loop
    
    ' fill array
    Dim tokens() As Variant
    tokens = MainModule.CollectTripleTokens(beginCell, count)
    
    GetCompanyTokens = tokens
    
End Function

Function GetAssignmentTokens(assignmentId As String) As Variant
    
    Sheet2.Select
        
    Dim cell As Range, beginCell As Range, count As Integer
    Set beginCell = FindAssignmentBeginCell(assignmentId)
    Set cell = beginCell
    count = 0
    
    ' tokens come after the assignment begin cell and before any cell with comment, count total
    Do While True
        Set cell = cell.Offset(1, 0)
        If cell.comment Is Nothing Then
            count = count + 1
        Else
            Exit Do
        End If
    Loop
    
    ' fill array
    Dim tokens() As Variant
    tokens = MainModule.CollectTripleTokens(beginCell, count)
    
    GetAssignmentTokens = tokens
    
End Function

Sub IndexAssignments(assign As Boolean)
    
    Sheet2.Select
    
    Dim cell As Range, lastCellRowNumber As Integer, index As Integer
    
    Columns("A:A").Select
    lastCellRowNumber = ActiveCell.Row
    Set cell = ActiveCell
    index = 0
    
    Do While True
        Set cell = Selection.Find(What:="AssignmentSection BEGIN", After:=cell, LookIn:=xlComments)
        If cell Is Nothing Then
            Exit Do
        ElseIf lastCellRowNumber >= cell.Row Then
            Exit Do
        Else
            index = index + 1
            lastCellRowNumber = cell.Row
            
            ' company ID (find cell by going up, cell must exist)
            Dim companyCell As Range, companyId As String
            Set companyCell = cell
            Do While True
                Set companyCell = companyCell.Offset(-1, 0)
                If companyCell.comment Is Nothing Then
                    ' look further up
                ElseIf companyCell.comment.text = "CompanySection BEGIN" Then
                    Exit Do
                End If
            Loop
            companyId = companyCell.Offset(0, 1).value
            
            ' assign or clear ID comment
            cell.Offset(0, 1).ClearComments
            If (assign) Then
                cell.Offset(0, 1).AddComment "AssignmentId:" + companyId + "_a" + CStr(index) + "!"
            End If
        End If
    Loop
    
End Sub

