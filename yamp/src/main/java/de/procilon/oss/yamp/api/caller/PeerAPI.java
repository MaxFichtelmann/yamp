package de.procilon.oss.yamp.api.caller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import de.procilon.oss.yamp.api.shared.Request;
import de.procilon.oss.yamp.api.shared.Response;
import de.procilon.oss.yamp.serialization.CredentialContainer;
import de.procilon.oss.yamp.serialization.Message;
import de.procilon.oss.yamp.serialization.RequestContainer;
import de.procilon.oss.yamp.serialization.ResponseContainer;
import lombok.RequiredArgsConstructor;

/**
 * @author wolffs
 */
@RequiredArgsConstructor
public class PeerAPI
{
    private final Transport                              transport;
    private final Function<Message, CredentialContainer> authenticator;
    
    public <T extends Response> CompletionStage<T> process( Request<T> request, Function<Message, T> decoder )
    {
        Message requestMessage = request.toMessage();
        RequestContainer requestContainer = new RequestContainer( requestMessage.encode(), authenticator.apply( requestMessage ) );
        
        return transport.transmit( requestContainer.encode() )
                .thenCompose( handled( ResponseContainer::decode ) )
                .thenApply( ResponseContainer::getMessage )
                .thenCompose( handled( Message::decode ) )
                .thenCompose( handled( decoder::apply ) );
    }
    
    private static <T, R> Function<T, CompletionStage<R>> handled( Function<T, R> f )
    {
        return input -> {
            try
            {
                return CompletableFuture.completedStage( f.apply( input ) );
            }
            catch ( RuntimeException e )
            {
                return CompletableFuture.failedStage( e );
            }
        };
    }
}
