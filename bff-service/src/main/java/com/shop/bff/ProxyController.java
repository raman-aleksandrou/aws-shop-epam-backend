package com.shop.bff;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * Catch-all proxy. Forwards {bff-url}/{service-name}/...?query to the base URL
 * configured for {service-name} in the .env file, using the same HTTP method,
 * and returns the recipient's response (status code + body) unchanged.
 */
@RestController
public class ProxyController {

    /** Hop-by-hop headers that must not be forwarded. */
    private static final Set<String> SKIP_REQUEST_HEADERS =
            Set.of("host", "content-length", "connection", "accept-encoding");
    private static final Set<String> SKIP_RESPONSE_HEADERS =
            Set.of("transfer-encoding", "content-length", "connection");

    private final ServiceUrlResolver resolver;
    private final RestClient restClient = RestClient.create();

    public ProxyController(ServiceUrlResolver resolver) {
        this.resolver = resolver;
    }

    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                        @RequestBody(required = false) byte[] body) {

        // Split "/product/123" -> service="product", remainder="/123"
        String uri = request.getRequestURI();
        String[] parts = uri.split("/", 3);
        String serviceName = parts.length > 1 ? parts[1] : "";
        String remainder = parts.length > 2 ? "/" + parts[2] : "";

        Optional<String> baseUrl = resolver.resolve(serviceName);
        if (baseUrl.isEmpty()) {
            return ResponseEntity.status(502)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Cannot process request".getBytes());
        }

        String query = request.getQueryString();
        String target = baseUrl.get() + remainder + (query != null ? "?" + query : "");

        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        try {
            RestClient.RequestBodySpec spec = restClient.method(method).uri(URI.create(target));

            // Forward request headers, skipping hop-by-hop ones.
            Collections.list(request.getHeaderNames()).forEach(name -> {
                if (!SKIP_REQUEST_HEADERS.contains(name.toLowerCase())) {
                    spec.header(name, Collections.list(request.getHeaders(name)).toArray(String[]::new));
                }
            });

            if (body != null && body.length > 0) {
                spec.body(body);
            }

            // exchange() returns the recipient's response as-is, without throwing on 4xx/5xx.
            return spec.exchange((req, res) -> {
                byte[] responseBody = res.getBody() != null ? res.getBody().readAllBytes() : new byte[0];
                HttpHeaders headers = new HttpHeaders();
                res.getHeaders().forEach((key, values) -> {
                    // Skip hop-by-hop headers and HTTP/2 pseudo-headers (":status", ":path", ...),
                    // which are invalid as HTTP/1.1 header names and break the response.
                    if (!key.startsWith(":") && !SKIP_RESPONSE_HEADERS.contains(key.toLowerCase())) {
                        headers.put(key, values);
                    }
                });
                return ResponseEntity.status(res.getStatusCode()).headers(headers).body(responseBody);
            });
        } catch (Exception ex) {
            // Recipient unreachable / network failure.
            return ResponseEntity.status(502)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Cannot process request: " + ex.getMessage()).getBytes());
        }
    }
}
