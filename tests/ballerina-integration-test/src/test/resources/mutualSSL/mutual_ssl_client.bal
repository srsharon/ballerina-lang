import ballerina/http;
import ballerina/io;
import ballerina/mime;

endpoint http:Client clientEP {
    url:"https://localhost:9116",
    secureSocket:{
        keyStore:{
            path:"${ballerina.home}/bre/security/ballerinaKeystore.p12",
            password:"ballerina"
        },
        trustStore:{
            path:"${ballerina.home}/bre/security/ballerinaTruststore.p12",
            password:"ballerina"
        },
        protocol:{
            name:"TLSv1.2",
            versions:["TLSv1.2", "TLSv1.1"]
        },
        ciphers:["TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"],
        certValidation: {
            enable: false
        },
        ocspStapling: false
    }
};

public function main (string... args) {
    http:Request req = new;
    var resp = clientEP -> get("/echo/");
    match resp {
        error err => io:println(err.reason());
        http:Response response => {
             match (response.getTextPayload()) {
                error payloadError => io:println(payloadError.reason());
                string res => io:println(res);
             }
        }
    }
}