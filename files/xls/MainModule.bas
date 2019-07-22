Attribute VB_Name = "MainModule"
Sub Init()

    ControlsModule.Init
    PersonModule.Init
    CareerModule.Init
    ExportModule.Init
    
    Sheet1.Select
    Range("A1").Select
    
End Sub

Function IsArrayAllocated(arr As Variant) As Boolean
    ' function to check if array is defined and not empty
    
    On Error Resume Next
    IsArrayAllocated = IsArray(arr) And _
                       Not IsError(LBound(arr, 1)) And _
                       LBound(arr, 1) <= UBound(arr, 1)
End Function

Function CollectTripleTokens(afterCell As Range, count As Integer) As Variant
    ' function to collect key and value as triple predicate and object

    Dim tokens() As Variant, i As Integer, cell As Range

    If count = 0 Then
        ' no tokens found
    Else
        Set cell = Range(afterCell.Offset(1, 0), afterCell.Offset(count + 1, 0))
        ReDim tokens(1 To count, 1 To 2) As Variant
        For i = 1 To count
            tokens(i, 1) = cell(i, 2).value
            tokens(i, 2) = cell(i, 3).value
        Next i
    End If

    CollectTripleTokens = tokens

End Function

