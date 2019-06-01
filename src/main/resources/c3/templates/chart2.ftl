c3.generate({
  bindto: "#chart2",
  data: {
    type: "bar",
    columns: ${js_columns},
    groups: ${js_groups},
    order: null,
    colors: ${js_colors}
  },
  axis: {
    rotated: true,
    x: {
      label: {
        text: "Companies",
        position: "outer-middle"
      },
      tick: {
        format: function (x) {
          return ""
        }
      }
    },
    y: {
      label: "Time lapse",
      tick: {
        count: 1,
        format: function (x) {
          return ""
        }
      }
    }
  },
  tooltip: {
    grouped: false,
    format: {
      value: function (value, ratio, id, index) {
        return value+" months"
      },
      name: function (name, ratio, id, index) {
        return id.startsWith("(gap ") ? "" : name
      },
      title: function (x, index) {
        return ""
      }
    }
  },
  legend: {
    hide: ${js_legend_hide}
  },
  size: {
    width: 800
  }
});
