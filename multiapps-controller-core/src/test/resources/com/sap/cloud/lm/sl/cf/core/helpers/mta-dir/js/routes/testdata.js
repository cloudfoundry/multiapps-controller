var express = require('express');
var router = express.Router();
var testdataCreator = require('./testdataCreator');
var testdataDestructor = require('./testdataDestructor');
router.get('/rest/addressbook/testdata', function (req, res) {
    testdataCreator(req.db, req.user.id, function(error) {
        if (error) {
            res.writeHead(500, {'Content-Type': 'application/json'});
            console.error(error);
            res.end('{}');
        } else {
            res.writeHead(200, {'Content-Type': 'application/json'});
            res.end(JSON.stringify({ status: 'success'}));
        }
    });
});
router.get('/rest/addressbook/testdataDestructor', function (req, res) {
    testdataDestructor(req.db, req.authInfo, function(error) {
        if (error) {
            res.writeHead(403, {'Content-Type': 'application/json'});
            console.error(error);
            res.end('{}');
        } else {
            res.writeHead(200, {'Content-Type': 'application/json'});
            res.end(JSON.stringify({ status: 'success'}));
        }
    });
});
module.exports = router;
