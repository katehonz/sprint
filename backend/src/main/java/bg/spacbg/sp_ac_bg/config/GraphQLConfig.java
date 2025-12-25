package bg.spacbg.sp_ac_bg.config;

import graphql.scalars.ExtendedScalars;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.web.multipart.MultipartFile;

@Configuration
public class GraphQLConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.DateTime)
                .scalar(uploadScalar());
    }

    private GraphQLScalarType uploadScalar() {
        return GraphQLScalarType.newScalar()
                .name("Upload")
                .description("A file upload scalar")
                .coercing(new Coercing<MultipartFile, Void>() {
                    @Override
                    public Void serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        throw new CoercingSerializeException("Upload is an input-only type");
                    }

                    @Override
                    public MultipartFile parseValue(Object input) throws CoercingParseValueException {
                        if (input instanceof MultipartFile) {
                            return (MultipartFile) input;
                        }
                        throw new CoercingParseValueException("Expected a MultipartFile");
                    }

                    @Override
                    public MultipartFile parseLiteral(Object input) throws CoercingParseLiteralException {
                        throw new CoercingParseLiteralException("Upload cannot be used as a literal");
                    }
                })
                .build();
    }
}
