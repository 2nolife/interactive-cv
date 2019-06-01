<!DOCTYPE html>
<html lang="en">
  <head>

    <meta charset="UTF-8">
    <title>${html_title} | Interactive CV</title>
    <link rel="icon" type="image/png" href="etc/icon.png" />

    <link href="libs/c3.min.css" rel="stylesheet">
    <link href="etc/main.css" rel="stylesheet">

  </head>
  <body>

    <header>
      <h1>${html_title}</h1>
      <p>Generated ${html_generated}</p>
    </header>

    <div class="charts">
      <div id="chart1" class="chart"></div>
      <div id="chart3" class="chart"></div>
      <div id="chart2" class="chart"></div>
    </div>

    <footer>
      <a href="https://github.com/2nolife/interactive-cv" title="Visit GitHub project page"><img src="etc/GitHub-Mark-Light-64px.png" /></a>
    </footer>

    <script src="libs/d3.min.js" charset="utf-8"></script>
    <script src="libs/c3.min.js"></script>
    <script src="${html_person_id}.js"></script>

  </body>
</html>