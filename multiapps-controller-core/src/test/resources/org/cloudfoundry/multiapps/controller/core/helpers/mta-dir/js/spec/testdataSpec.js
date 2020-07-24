var sap_hdb_conn = require('sap-hdb-connection');
var testdataCreator = require('../routes/testdataCreator')

describe("testing data creation", function(done) {
    it("can create adress books and their entries", function(done) {
        sap_hdb_conn.createConnection(function(error, client) {
            expect(error).toBe(null)
            expect(client).not.toBe(null)

            testdataCreator(client, "myuserid", function(error) {
                expect(error).toBe(null) //TODO: why is error undefined and not null?
                client.exec("SELECT * from \"com.sap.xs2.samples::AddressBook.Book\" WHERE \"name\" LIKE '%by myuserid'", function(error, rows) {
                    expect(error).toBe(null)

                    /**
                     * Actually it would be better to validate the query against the data entered by
                     * testdataCreator. However the testdataCreator does not report the ID of the 
                     * inserted data. So we can just validate that something is int he table..
                     * As long as no error is reported the operation is expected to be successful.
                     */
                    expect(rows.length).toBeGreaterThan(0)
                    done()
                })
            })
        })
    })
})
