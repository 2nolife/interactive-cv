## Extension to C3 writer for Neovis graph visualization ##

### overrides.conf ###
```
interceptors = [
  <list of interceptors>
  "com.coldcore.icv.interceptor.c4.Default"
]

c3-writer = {
  dir = "target/output/c4-resources"
  charts = [
    <list of charts>
    "com.coldcore.icv.writer.c4.chart.Neovis"
  ]
}
```
