var express = require('express')
var request = require('request')

var app = express()

var config = require('./config.json')

function log(msg) {
  if (config.log) console.log(msg)
}

function sendFile(res, url) {
  var options = { root : __dirname, dotfiles: 'deny' }
  res.sendFile('public/'+url, options, function (err) {
    if (err) {
      log(err);
      res.status(err.status).end()
    }
  })
}

app.use("/", function(req, res) {
  var url = req.url.substring(1)
  if (url == '') url = config.landing
  if (url.indexOf('.') != -1) {
    log('Static file '+url)
    sendFile(res, url)
  } else {
    var cv = decodeURI(url.substring(url.lastIndexOf('/')+1))
    log('Requested '+cv)

    var html = cv.toLowerCase()+'.html'
    html = html.replace(' ', '_')
    html = html.replace('-', '_')
    log('HTML '+html)
    
    sendFile(res, html)
  }
})

app.listen(config.port, function() {
  console.log('Bound weblet to port '+config.port)
})
