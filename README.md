<pre>
 _____ _      ____ ____  ____  ____
/  __// \  /|/   _Y  _ \/  __\/ ___\
|  \  | |\ |||  / | / \||  \/||    \
|  /_ | | \|||  \_| \_/||    /\___ |
\____\\_/  \|\____|____/\_/\_\\____/
</pre>

Yet again another [CORS](http://www.w3.org/TR/cors/) library for ring.

[![Build Status](https://travis-ci.org/unbounce/encors.svg?branch=master)](https://travis-ci.org/unbounce/encors)
<br/>
[![Clojars Project](http://clojars.org/com.unbounce/encors/latest-version.svg)](http://clojars.org/com.unbounce/encors)

## Usage

Add CORS support to ring apps, this middleware provides:

### `wrap-cors`

Receives 2 arguments:

  1) The ring app being wrapped.


  2) A map that _must_ have the following keys:

  - `:allowed-origins`

    A set that specifies which origins are allowed by the
    middleware. A value of `:star-origin` indicates unrestricted cross-origin
    sharing and results in `*` as value for the
    `Access-Control-Allow-Origin` HTTP response header.
    A value of `:match-origin` will always return the incoming origin header.

  - `:allowed-methods`

    A set that specifies the HTTP methods allowed in CORS requests.
    (valid values are [here](https://github.com/unbounce/encors/blob/master/src/com/unbounce/encors/types.clj#L17))

  - `:request-headers`

    A set of field names of HTTP request headers that are allowed in
    CORS requests.  Some headers found on a simple CORS implementation
    are included implicitly (except `Content-Type`)

  - `:exposed-headers`

    A set of HTTP header field names that will be exposed on the
    client (can be nil).

  - `:max-age`

    Number of seconds that the response may be cached by the client
    (can be nil).

  - `:allow-credentials?`

    A boolean that if `true`, adds the
    `Access-Control-Allow-Credentials` header on preflight requests.

  - `:origin-varies?`

    If the resource is shared by multiple origins but
    `Access-Control-Allow-Origin` is not set to `*` this may be set to
    `true`.

  - `:require-origin?`

    If this is `true` and the request does not include an `Origin`
    header the response will have HTTP status 400 (bad request) and
    the body will contain a short error message.

  - `:ignore-failures?`

    In case that:

      *  the request contains an `Origin` header and

      *  the client does not conform with the CORS protocol
         (__request is out of scope__)

    then

      * the request is passed on unchanged to the application if this
        field is `true` or

      * a response with HTTP status 400 (bad request) and short error
        message will be returned if this field is `false`


Example:

```clojure
(ns my.ring-app
  (:require
    [com.unbounce.encors :refer [wrap-cors]]
    ;; ... other misc ring imports
    )

(defn raw-app [req]
  ;; return response here
  )

(def cors-policy
     { :allowed-origins #{"example.com"}
       :allowed-methods #{:get}
       :request-headers #{"X-Example-Header"}
       :exposed-headers nil
       :allow-credentials? true
       :origin-varies? false
       :max-age nil
       :require-origin? true
       :ignore-failures? false
     })

(def app (wrap-cors raw-app cors-policy))
```

## License

Copyright © 2014-2015 Unbounce Marketing Solutions Inc.

Distributed under the [MIT License (MIT)](http://opensource.org/licenses/MIT).
