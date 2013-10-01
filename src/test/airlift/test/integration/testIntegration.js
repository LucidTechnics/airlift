var print = function print(_message)
{
	Packages.java.lang.System.out.println(_message);
};

var util = require('airlift/util');
var assert = require('airlift/assert');
var easyhandle = require('airlift/easyhandle');

function resourceConfirm(entry, _assert, _testName) {
	var name = _testName||'test name not provided';
    _assert.ok(entry.id, name + ' There is an error with the HTTP response output (ID).');
    _assert.ok(entry.fullName, name + ' There is an error with the HTTP response output (fullName).');
    _assert.ok(entry.password, name + ' There is an error with the HTTP response output (password).');
	_assert.ok(entry.emailAddress, name + ' There is an error with the HTTP response output (emailAddress).');
	_assert.ok(entry.mobilePhoneNumber, name + ' There is an error with the HTTP response output (mobilePhoneNumber).');
    _assert.ok(entry.emailAddress.contains("@"), name + ' The Email Address is malformed'); //Email address may exist but may still be malformed
    _assert.eq(entry.id.length, 12, name + ' The ID is malformed'); // ID may exist but may still be malformed
};

function parseJson(_json)
{
	util.info('parsing', _json);
	var parsedResult;
	
	if (_json && _json.length > 0)
	{
		parsedResult = JSON.parse(_json);
	}

	return parsedResult;
}

exports['setUp'] = function(_assert) //Just as we test multiple GET requests with COLLECT, this test it to ensure that multiple POST requests can be conducted in quick succession.  In addition, this is used to populate the database for use with testDELETE/PUT/GET later.
{
	var baseUrl = "http://localhost:8080/a/registration";
	
    var registration1 = Packages.java.lang.String("fullName=SamJones&emailAddress=emailTest%40example.com&mobilePhoneNumber=9999999999&password=AAAAAAAA");
    var registration2 = Packages.java.lang.String("fullName=Dafna&emailAddress=emailTest2%40example.com&mobilePhoneNumber=9999999999&password=BBBBBBBB");
    var registration3 = Packages.java.lang.String("fullName=John&emailAddress=emailTest3%40example.com&mobilePhoneNumber=0000000000&password=1222111222");
    var registration4 = Packages.java.lang.String("fullName=MIKE&emailAddress=myemail%40example.com&mobilePhoneNumber=3213213213&password=AAAAAAAA");

    var responseCode = easyhandle.serverPost(baseUrl, registration1, _assert);
    _assert.eq(responseCode, 200, ('The registration1 POST request did not return the proper response code (' + responseCode + ')'));
    responseCode = easyhandle.serverPost(baseUrl, registration2, _assert);
    _assert.eq(responseCode, 200, ('The registration2 POST request did not return the proper response code (' + responseCode + ')'));
    responseCode = easyhandle.serverPost(baseUrl, registration3, _assert);
    _assert.eq(responseCode, 200, ('The registration3 POST request did not return the proper response code (' + responseCode + ')'));
    responseCode = easyhandle.serverPost(baseUrl, registration4, _assert);
    _assert.eq(responseCode, 200, ('The registration4 POST request did not return the proper response code (' + responseCode + ')'));
};

exports['test COLLECT'] = function(_assert)
{
	var resultArray = parseJson(easyhandle.serverCollect("http://localhost:8080/a/registration", true, _assert));
	
    _assert.ok(resultArray, 'registration COLLECT was unsuccessful');

	resultArray.forEach(function(entry)
	{
		resourceConfirm(entry, _assert, 'COLLECT');
    });

    _assert.eq(resultArray.length, 4, 'Did not get expected number of resources');
};

exports['test GET'] = function(_assert)
{
	var resultArrayCollect = parseJson(easyhandle.serverCollect("http://localhost:8080/a/registration", false, _assert));
	
	_assert.ok(resultArrayCollect, 'GET COLLECT was unsuccessful');
	
    var urlBase = "http://localhost:8080/a/registration/" + resultArrayCollect[0].id;
    var resource = parseJson(easyhandle.serverGet(urlBase, true, _assert));      
	_assert.ok(resource, 'GET was unsuccessful');
	_assert.eq(resultArrayCollect[0].id, resource.id, 'Wrong resource was returned by GET');
	
    resourceConfirm(resource, _assert, 'GET');
};

/*exports['test DELETE'] = function(_assert)
{
    var resultArrayCollect = parseJson(easyhandle.serverCollect("http://localhost:8080/a/registration", false, _assert));
    _assert.ok(resultArrayCollect, 'DELETE COLLECT was unsuccessful');
    var urlBase = "http://localhost:8080/a/registration/" + resultArrayCollect[1].id;
    var responseCode = easyhandle.serverDelete(urlBase, _assert);

    var resource = parseJson(easyhandle.serverGet(urlBase, false, _assert));
    _assert.ok(!resource, 'DELETE was unsuccessful, resource still exists in array');
    _assert.eq(responseCode, 200, ('The HTTP DELETE request did not return the proper response code (' + responseCode + ')'));
};

exports['test PUT'] = function(_assert)
{
    var resultArrayCollect = parseJson(easyhandle.serverCollect("http://localhost:8080/a/registration", false, _assert));
    _assert.ok(resultArrayCollect, 'PUT COLLECT was unsuccessful');

	var urlBase = "http://localhost:8080/a/registration/" + resultArrayCollect[2].id;
    var data = "fullName=Sample&emailAddress=testEmail2%40example.com&mobilePhoneNumber=1231231234&password=new1112d54";
    var responseCode = easyhandle.serverPut(urlBase, data, _assert);
    _assert.eq(responseCode, 200, ('The HTTP PUT request did not return the proper response code (' + responseCode + ')'));

    var resource = parseJson(easyhandle.serverGet(urlBase, false, _assert));
    _assert.ok(resource, 'PUT GET was unsuccessful');

    resourceConfirm(resource, _assert, 'PUT');

	_assert.eq("Sample", resource.fullName, "The fullName was not properly PUT");
    _assert.eq("testEmail2@example.com", resource.emailAddress, "The email address was not properly PUT");
    _assert.eq("new1112d54", resource.password, "The password was not properly PUT");
    _assert.eq("1231231234", resource.mobilePhoneNumber, "The mobilePhoneNumber was not properly PUT");
};

exports['test POST'] = function(_assert)
{
    var baseUrl = "http://localhost:8080/a/registration";
    var data = Packages.java.lang.String("fullName=JohnDoe&emailAddress=emailTest%40example.com&mobilePhoneNumber=9999999999&password=AAAAAAAA");
    var responseCode = easyhandle.serverPost(baseUrl, data, _assert);
    _assert.eq(responseCode, 200, ('The HTTP POST request did not return the proper response code (' + responseCode + ')'));

    var resultArrayCollect = parseJson(easyhandle.serverCollect("http://localhost:8080/a/registration", false, _assert));
    _assert.ok(resultArrayCollect, 'POST COLLECT was unsuccessful');
    var urlBase = "http://localhost:8080/a/registration/" + resultArrayCollect[(resultArrayCollect.length-1)].id;
    var resource = parseJson(easyhandle.serverGet(urlBase, false, _assert));      
    _assert.ok(resource, 'POST GET was unsuccessful');

	resourceConfirm(resource, _assert);
	
    _assert.eq("JohnDoe", resource.fullName, "The fullName was not properly POSTed");
    _assert.eq("emailTest@example.com", resource.emailAddress, "The email address was not properly POSTed");
    _assert.eq("AAAAAAAA", resource.password, "The password was not properly POSTed");
    _assert.eq("9999999999", resource.mobilePhoneNumber, "The mobilePhoneNumber was not properly POSTed");
};*/

exports['tearDown'] = function(_assert)
{
	var resultArrayCollect = parseJson(easyhandle.serverCollect("http://localhost:8080/a/registration", false, _assert));
	_assert.ok(resultArrayCollect, 'First tear down COLLECT was unsuccessful');

	resultArrayCollect.forEach(function(_resource)
	{
		util.info('deleting', _resource.id);
		
		var urlBase = "http://localhost:8080/a/registration/" + _resource.id;
		var responseCode = easyhandle.serverDelete(urlBase, _assert);
	});

	resultArrayCollect = parseJson(easyhandle.serverCollect("http://localhost:8080/a/registration", false, _assert));
	_assert.eq(resultArrayCollect.length, 0, 'Last tear down COLLECT was unsuccessful');
};