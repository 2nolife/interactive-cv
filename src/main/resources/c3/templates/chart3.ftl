c3.generate({
  bindto: "#chart3",
  data: {
    columns: ${js_columns},
    type : "donut"
  },
  donut: {
    title: "Experience",
    label: {
      format: function (value, ratio, id) {
        return value+" m"
      }
    }
  },
  tooltip: {
    format: {
      value: function (value, ratio, id, index) {
        return value+" months"
      }
    }
  },
  size: {
    width: 300
  }
});
