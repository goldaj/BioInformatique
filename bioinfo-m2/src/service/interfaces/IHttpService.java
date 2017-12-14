package service.interfaces;

import com.google.api.client.http.HttpResponse;
import com.google.common.util.concurrent.ListenableFuture;

public interface IHttpService {
    ListenableFuture<HttpResponse> get(final String url);
    ListenableFuture<HttpResponse> get(final String url, final String geneId);
}
