import ballerina/http;

type UserInfo record {|
   string username;
   string password;
|};

type Student record {|
   string username;
   string password;
   string 'record;
|};

const string CONST = "CONST";
Student myStudent = {};

service OASServiceType on new http:Listener(9090) {

	resource function get pet() returns int|http:NotFound {
        do {
            Student student = {};
		} on fail error e {
			return http:NOT_FOUND;
		}
	}
}
