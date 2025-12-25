package bg.spacbg.sp_ac_bg.config;

import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * GraphQL Security Interceptor - ensures all GraphQL operations require authentication
 * except for explicitly allowed public operations.
 */
@Component
public class GraphQLSecurityInterceptor implements WebGraphQlInterceptor {

    // Public operations that don't require authentication
    private static final Set<String> PUBLIC_OPERATIONS = Set.of(
            "login",
            "recoverPassword",
            "resetPassword",
            "__schema",
            "__type"
    );

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String operationName = request.getOperationName();
        String document = request.getDocument();

        // Check if this is a public operation
        boolean isPublicOperation = isPublicOperation(operationName, document);

        if (!isPublicOperation) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return Mono.error(new AuthenticationCredentialsNotFoundException("Unauthorized: Authentication required"));
            }
        }

        return chain.next(request);
    }

    private boolean isPublicOperation(String operationName, String document) {
        // Check explicit operation name
        if (operationName != null && PUBLIC_OPERATIONS.contains(operationName)) {
            return true;
        }

        // Check document for public mutations/queries
        if (document != null) {
            String lowerDoc = document.toLowerCase();
            for (String op : PUBLIC_OPERATIONS) {
                if (lowerDoc.contains(op.toLowerCase())) {
                    return true;
                }
            }
        }

        return false;
    }
}
