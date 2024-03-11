var async = require('async');
var cds = require('cds');
var winston = require('winston');

var BOOK;
var ADDRESS;

winston.level = process.env.winston_level || 'error';

function init(appCallback) {
    if (BOOK && ADDRESS) {
        appCallback();
    } else {
        async.waterfall([
            function (callback) {
                cds.importEntities([ {
                        $entity: "com.sap.xs2.samples::AddressBook.Book",
                        $fields: { // for convenience we add an association from books to addresses
                            addresses: {
                                $association: {
                                    $entity: "com.sap.xs2.samples::AddressBook.Address",
                                    $viaBacklink: "book"
                                }
                            }
                        }
                    },
                    {$entity: "com.sap.xs2.samples::AddressBook.Address"}
                ], callback);
            },
            function (entities, callback) {
                BOOK = entities["com.sap.xs2.samples::AddressBook.Book"];
                ADDRESS = entities["com.sap.xs2.samples::AddressBook.Address"];
                callback(null);
            }
        ], function (error) {
            appCallback(error);
        });
    }
}

function testdataCreator(dbConnection, userid, appCallback) {
    var conn = null;
    async.waterfall([
        init,
        function connect(cb) {
            // reuse connection attached to HTTP request
            cds.$getTransaction(dbConnection, cb);
        },
        function store(tx, cb) {
            conn = tx;
            cb(null);
        },
        function createBook(cb) {
            var bookId = Math.floor(Math.random() * 1000000);
            conn.$save({
                $entity: BOOK,
                id: bookId,
                name: 'My Book #' + bookId + ' created by ' + userid
            }, cb);
        },
        function createAddresses(book, cb) {
            conn.$saveAll([
                {
                    $entity: ADDRESS,
                    id: Math.floor(Math.random() * 1000000),
                    book: book,
                    first_name: 'John',
                    last_name: 'Doe',
                    address: 'Dietmar-Hopp-Allee 16',
                    city: 'Walldorf',
                    country: 'Germany',
                    zip: '69169',
                    phone: '+49 6227 7 12345',
                    email: 'john.doe@sap.com',
                    web: 'https://sap.de'
                },
                {
                    $entity: ADDRESS,
                    id: Math.floor(Math.random() * 1000000),
                    book: book,
                    first_name: 'Max',
                    last_name: 'Mustermann',
                    address: 'Dietmar-Hopp-Allee 16',
                    city: 'Walldorf',
                    country: 'Germany',
                    zip: '69169',
                    phone: '+49 6227 7 54321',
                    email: 'john.doe@sap.com',
                    web: 'https://sap.de'
                }
            ], cb);
        },
        function release(instances, cb) {
            conn.$close();
            cb(null);
        }
    ], appCallback);
}

module.exports = testdataCreator;
