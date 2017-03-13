CREATE TABLE installations (
	shop_name VARCHAR(256),
	api_url VARCHAR(1024),
    email VARCHAR(256),
    email_confirmed BOOLEAN,
    access_token VARCHAR(2048),
    created DATE,
    PRIMARY KEY (api_url, access_token)
);