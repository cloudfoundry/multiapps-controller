var sap_hdb_conn = require('sap-hdb-connection');
var userinfoQuery = require('../routes/userinfoQuery')
describe("testing user info queries", function() {
    /**
     * in the fully integrated application the userinfoQuery application should
     * return the logged-in business user in APPLICATION_USER. Since this is only
     * a standalone test with all the SAML, JoT stuff in it we can only expect
     * the technical user.
     */
    it("can read the current, session and application user from DB", function(done) {
        sap_hdb_conn.createConnection(function(error, client) {
            expect(error).toBe(null)
            expect(client).not.toBe(null)
            userinfoQuery(null, client, function(error, result) {
                expect(result.hdbCurrentUser[0].CURRENT_USER).toEqual("NODEJS_HELLOWORLD_APP")
                expect(result.hdbCurrentUser[0].SESSION_USER).toEqual("NODEJS_HELLOWORLD_APP")
                expect(result.hdbCurrentUser[0].APPLICATION_USER).toEqual("NODEJS_HELLOWORLD_APP")
                done()
            })
        })
    })
})
