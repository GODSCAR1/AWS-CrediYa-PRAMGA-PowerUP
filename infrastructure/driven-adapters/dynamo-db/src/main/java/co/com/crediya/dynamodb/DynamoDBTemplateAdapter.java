package co.com.crediya.dynamodb;

import co.com.crediya.dynamodb.helper.TemplateAdapterOperations;
import co.com.crediya.model.ContadorGlobal;
import co.com.crediya.model.gateways.ContadorRepository;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.math.BigDecimal;


@Repository
public class DynamoDBTemplateAdapter extends TemplateAdapterOperations<ContadorGlobal, String, ContadorGlobalEntity> implements ContadorRepository {


    public DynamoDBTemplateAdapter(DynamoDbEnhancedAsyncClient connectionFactory, ObjectMapper mapper) {
        /**
         *  Could be use mapper.mapBuilder if your domain model implement builder pattern
         *  super(repository, mapper, d -> mapper.mapBuilder(d,ObjectModel.ObjectModelBuilder.class).build());
         *  Or using mapper.map with the class of the object model
         */
        super(connectionFactory, mapper, d -> mapper.map(d, ContadorGlobal.class), "crediya-reportes-dev");
    }


}
