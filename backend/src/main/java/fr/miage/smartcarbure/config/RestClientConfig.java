package fr.miage.smartcarbure.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Configure les clients HTTP utilisés pour appeler les API externes. */
@Configuration
public class RestClientConfig {

    /**
     * Client HTTP de l'API Prix des carburants (utilisé par CarburantApiClient).
     * Le nom du bean est spécifique pour ne pas entrer en conflit avec le client du service d'itinéraire.
     */
    @Bean
    public RestClient carburantRestClient(
            @Value("${smartcarbure.api-carburants.url}") String url,
            @Value("${smartcarbure.api-carburants.delai-connexion:3s}") Duration delaiConnexion,
            @Value("${smartcarbure.api-carburants.delai-lecture:5s}") Duration delaiLecture) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(delaiConnexion);
        requestFactory.setReadTimeout(delaiLecture);
        return RestClient.builder()
                .baseUrl(url)
                .requestFactory(requestFactory)
                .build();
    }
}
