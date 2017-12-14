package service.impl;

import com.google.api.client.http.*;
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import service.interfaces.ApiStatus;
import service.interfaces.IHttpService;
import service.interfaces.IProgramStatsService;
import service.interfaces.IProgressService;

import java.io.*;

public class HttpServiceImpl implements IHttpService {
    private final HttpRequestFactory requestFactory;
    private final RateLimiter rateLimiter;
    private final ListeningExecutorService executorService;
    private final IProgramStatsService programStatsService;
    private final IProgressService progressService;

    private boolean apiTrouble = false;

    @Inject
    public HttpServiceImpl(HttpTransport transport, RateLimiter rateLimiter, @Named("HttpExecutor") ListeningExecutorService listeningExecutorService, IProgramStatsService programStatsService, IProgressService progressService) {

    	this.requestFactory = transport.createRequestFactory(new HttpRequestInitializer());
        this.rateLimiter = rateLimiter;
        this.executorService = listeningExecutorService;
        this.programStatsService = programStatsService;
        this.progressService = progressService;
    }

    public ListenableFuture<HttpResponse> get(final String url) {
        return get(url, null);
    }

    public ListenableFuture<HttpResponse> get(final String url, final String geneId) {
        ListenableFuture<HttpResponse> responseFuture = executorService.submit(() -> {
            rateLimiter.acquire();
            //System.out.println("Request : " + "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=nuccore&id="+ geneId +"&rettype=fasta_cds_na&retmode=text");
            if (geneId != null) {
                progressService.getCurrentDownloadProgress().setDownloading(geneId);
                progressService.invalidateDownloadProgress();
            }
            GenericUrl genericUrl = new GenericUrl(url);
            HttpRequest request = requestFactory.buildGetRequest(genericUrl);
            return request.execute();
        });
        ListenableFuture<HttpResponse> failureCatchingFuture = Futures.catchingAsync(responseFuture, Throwable.class, exception -> {
            if (exception != null) {
                progressService.getCurrentApiStatus().setMessage("	");
                apiTrouble = true;
                progressService.getCurrentApiStatus().setColor(ApiStatus.OFFLINE_COLOR);
                progressService.invalidateApiStatus();
                exception.printStackTrace();
            }
            //return get(url, geneId);
            return null;
        }, executorService);

        return Futures.transformAsync(failureCatchingFuture, httpResponse -> {
            if(!apiTrouble) {
                progressService.getCurrentApiStatus().setMessage("API Online");
                progressService.getCurrentApiStatus().setColor(ApiStatus.ONLINE_COLOR);
            } else {
                progressService.getCurrentApiStatus().setMessage("API Online - Could not get all data (server issues)");
                progressService.getCurrentApiStatus().setColor(ApiStatus.TROUBLE_COLOR);
            }
            progressService.invalidateApiStatus();
            if (httpResponse == null) {
                return get(url);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(httpResponse.getContent()));
            br.mark(1);
            int firstByte = br.read();
            if (firstByte == -1) {
                return get(url);
            }

            br.reset();

            return Futures.immediateFuture(httpResponse);
        }, executorService);
    }
}