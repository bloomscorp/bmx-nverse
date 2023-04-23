package com.bloomscorp.nverse.support;

public final class Constant {

    private Constant() {}

    public static final String SCOPE_SINGLETON = "singleton";
    public static final String BLANK_STRING_VALUE = "";
    public static final String ENCODING_UTF_8 = "UTF-8";
    public static final String END_OF_STRING_VALUE = "\0";
    public static final String REQUEST_HEADER_AUTHORIZATION = "Authorization";
    public static final String REQUEST_HEADER_VALUE_BEARER = "Bearer ";

    /*--CORS METHOD CONSTANTS--*/

    public static final String METHOD_GET = "GET";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_DELETE = "DELETE";
    public static final String METHOD_OPTIONS = "OPTIONS";
    public static final String METHOD_HEAD = "HEAD";

    /*--CORS HEADERS CONSTANTS--*/

    public static final String HEADER_X_REQUESTED_WITH = "X-Requested-With";
    public static final String HEADER_ALTERNATES = "Alternates";
    public static final String HEADER_DESCRIPTION = "Content-Description";
}
