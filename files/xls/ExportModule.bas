Attribute VB_Name = "ExportModule"
Private CurrentRowNumber As Integer
Private PersonSectionNames(1 To 8) As String
Private UserId As String
Private PersonUri As String

Sub Init()

    
End Sub

Sub OnCellDoubleClick(cell As Range)

    ' do export
    If cell.Address = "$F$1" Then
        ExportForm.Show
    End If

End Sub

Sub ExportAsNTriples()

    CurrentRowNumber = 0
    
    ' person section names
    PersonSectionNames(1) = "Hobby"
    PersonSectionNames(2) = "About"
    PersonSectionNames(3) = "Available"
    PersonSectionNames(4) = "Contact"
    PersonSectionNames(5) = "Education"
    PersonSectionNames(6) = "Reference"
    PersonSectionNames(7) = "Skill"
    PersonSectionNames(8) = "Contribution"

    Progress "Preparing"

    Sheets("Person").Select
    UserId = Range("B1").value
    PersonUri = "<http://coldcore.com/schema/cv/Person/" + UserId + ">"

    Sheet3.Select
    Columns("A:D").ClearContents

    WritePerson
    WritePersonSections
    
    Progress "Indexing"
    CareerModule.IndexAssignments True
    
    WriteCareer
    WriteCompanies
    
    Progress "Finalising"
    CareerModule.IndexAssignments False

    Sheet3.Select
    Range("A1").CurrentRegion.Select
    
    Progress "Complete"
    
End Sub

Private Sub Progress(msg As String)
    
    ExportForm.ProgressLabel.Caption = msg
    DoEvents

End Sub

Private Function ConvertValueByUriType(value As String, uriType As String) As String
    
    Sheet4.Select

    Dim convertedValue As String

    If Left(value, 1) = "_" Or Left(value, 1) = "<" Then
        ' ignore anonymous or URI
        convertedValue = value
    Else
        
        Select Case uriType
            Case "date"
                convertedValue = """" + value + """" + "^^<http://www.w3.org/2001/XMLSchema#date>"
            Case "year"
                convertedValue = """" + value + """" + "^^<http://www.w3.org/2001/XMLSchema#gYear>"
            Case "month"
                convertedValue = """--" + value + """" + "^^<http://www.w3.org/2001/XMLSchema#gMonth>"
            Case "technology"
                convertedValue = "<http://coldcore.com/schema/cv/Technology/" + Replace(value, " ", "%20") + ">"
            Case "team"
                convertedValue = "<http://coldcore.com/schema/cv/Team/" + Replace(value, " ", "%20") + ">"
            Case Else
                convertedValue = """" + value + """"
        End Select
        
    End If

    ConvertValueByUriType = convertedValue
    
End Function

Private Sub WriteTruple(subject As String, predicate As String, object As String, Optional sectionName As String)

    If Left(predicate, 1) <> "<" Then
        ' convert predicate to URI and object to proper value (by predicate URI type)
        Dim uriType As String
        predicate = ControlsModule.GetUriByValue(predicate, sectionName)
        uriType = ControlsModule.GetUriTypeByUri(predicate)
        object = ConvertValueByUriType(object, uriType)
        'cut off _2 from the end
        If Right(predicate, 2) = "_2" Then
            predicate = Left(predicate, Len(predicate) - 2)
        End If
        predicate = "<" + predicate + ">"
    End If

    Sheet3.Select
    CurrentRowNumber = CurrentRowNumber + 1

    Dim cell As Range
    Set cell = Range("A" + CStr(CurrentRowNumber))
    
    cell.EntireRow.NumberFormat = "@"
    cell.value = subject
    cell.offset(0, 1).value = predicate
    cell.offset(0, 2).value = object
    cell.offset(0, 3).value = "."

End Sub

Private Sub WritePerson()

    Progress "Person"
    WriteTruple PersonUri, "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<http://xmlns.com/foaf/0.1/Person>"
        
End Sub

Private Sub WritePersonSections()

    Dim element As Variant
    For Each element In PersonSectionNames
    
        Dim tokens As Variant, i As Integer, j As Integer, tokenA As String, tokenB As String, sectionName As String
        sectionName = CStr(element)
        Progress "Person " + sectionName
        tokens = PersonModule.GetSectionTokens(sectionName)
        
        If MainModule.IsArrayAllocated(tokens) Then
            ' section header
            WriteTruple PersonUri, "<http://coldcore.com/schema/cv/term#" + LCase(sectionName) + ">", "_:" + LCase(sectionName)
            ' section values
            j = UBound(tokens, 1) - LBound(tokens, 1) + 1
            For i = 1 To j
                tokenA = CStr(tokens(i, 1))
                tokenB = CStr(tokens(i, 2))
                If tokenA <> "" And tokenB <> "" Then
                    WriteTruple "_:" + LCase(sectionName), tokenA, tokenB, UCase(sectionName)
                End If
            Next i
        End If

    Next element
    
End Sub

Private Sub WriteCareer()

    Progress "Career"
    WriteTruple PersonUri, "<http://coldcore.com/schema/cv/term#career>", "_:career"
        
End Sub

Private Sub WriteCompanies()

    Progress "Collecting assignments"
    Dim assignmentIds As Variant
    assignmentIds = CareerModule.GetAssignmentIds

    Progress "Collecting companies"
    Dim companyIds As Variant, i As Integer, j As Integer, companyId As String
    companyIds = CareerModule.GetCompanyIds
    
    If MainModule.IsArrayAllocated(companyIds) Then
        j = UBound(companyIds) - LBound(companyIds) + 1
        For i = 1 To j
            companyId = CStr(companyIds(i))
            Progress "Company " + companyId

            ' company header
            WriteTruple "_:career", "<http://coldcore.com/schema/cv/term#company>", "<http://coldcore.com/schema/cv/Company/" + companyId + ">"
            WriteTruple "<http://coldcore.com/schema/cv/Company/" + companyId + ">", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<http://xmlns.com/foaf/0.1/Organization>"
            ' company tokens
            WriteCompanyTokens companyId
            ' company assignments
            WriteAssignments companyId, assignmentIds
        Next i
    End If
    
End Sub

Private Sub WriteCompanyTokens(companyId As String)

    Dim tokens As Variant, i As Integer, j As Integer, tokenA As String, tokenB As String
    tokens = CareerModule.GetCompanyTokens(companyId)
    
    If MainModule.IsArrayAllocated(tokens) Then
        j = UBound(tokens, 1) - LBound(tokens, 1) + 1
        For i = 1 To j
            tokenA = CStr(tokens(i, 1))
            tokenB = CStr(tokens(i, 2))
            If tokenA <> "" And tokenB <> "" Then
                WriteTruple "<http://coldcore.com/schema/cv/Company/" + companyId + ">", tokenA, tokenB
            End If
        Next i
    End If
    
End Sub

Private Sub WriteAssignments(companyId As String, assignmentIds As Variant)

    Dim i As Integer, j As Integer, assignmentId As String
    
    If MainModule.IsArrayAllocated(assignmentIds) Then
        j = UBound(assignmentIds) - LBound(assignmentIds) + 1
        For i = 1 To j
            assignmentId = CStr(assignmentIds(i))
            Progress "Assignment " + assignmentId

            If InStr(1, assignmentId, companyId + "_a") = 1 Then
            ' assignment header
            WriteTruple "<http://coldcore.com/schema/cv/Company/" + companyId + ">", "<http://coldcore.com/schema/cv/term#assignment>", "_:" + assignmentId
            ' assignment tokens
            WriteAssignmentTokens assignmentId
            End If
        Next i
    End If
    
End Sub

Private Sub WriteAssignmentTokens(assignmentId As String)

    Dim tokens As Variant, i As Integer, j As Integer, tokenA As String, tokenB As String
    tokens = CareerModule.GetAssignmentTokens(assignmentId)
    
    If MainModule.IsArrayAllocated(tokens) Then
        j = UBound(tokens, 1) - LBound(tokens, 1) + 1
        For i = 1 To j
            tokenA = CStr(tokens(i, 1))
            tokenB = CStr(tokens(i, 2))
            If tokenA <> "" And tokenB <> "" Then
                WriteTruple "_:" + assignmentId, tokenA, tokenB, "ASSIGNMENT"
            End If
        Next i
    End If
    
End Sub

