package tn.lamg.gmao.web;

import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class VersionHeaders implements ResponseBodyAdvice<Object> {

    public boolean supports(
        MethodParameter parameter,
        Class<? extends HttpMessageConverter<?>> converter
    ) {
        return true;
    }

    public Object beforeBodyWrite(
        Object body,
        MethodParameter parameter,
        MediaType mediaType,
        Class<? extends HttpMessageConverter<?>> converter,
        ServerHttpRequest request,
        ServerHttpResponse response
    ) {
        if (body instanceof Map<?, ?> m && m.get("version") != null) {
            response.getHeaders().setETag("\"" + m.get("version") + "\"");
            if (
                response instanceof ServletServerHttpResponse servlet &&
                servlet.getServletResponse().getStatus() == 201 &&
                m.get("id") != null
            ) response
                .getHeaders()
                .setLocation(java.net.URI.create(request.getURI().getPath() + "/" + m.get("id")));
        }
        return body;
    }
}
