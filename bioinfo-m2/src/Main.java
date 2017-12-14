import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.RateLimiter;
import com.google.inject.*;
import com.google.inject.name.Named;

import controller.MainController;
import service.impl.*;
import service.interfaces.*;

import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import java.util.concurrent.Executors;

public class Main extends AbstractModule {
    @Override
    protected void configure() {
        bind(IHttpService.class).to(HttpServiceImpl.class).asEagerSingleton();
        bind(HttpTransport.class).to(NetHttpTransport.class).asEagerSingleton();
        bind(IParseService.class).to(ParseServiceImpl.class).asEagerSingleton();
        bind(IFileService.class).to(FileServiceImpl.class).asEagerSingleton();
        bind(IConfigService.class).to(ConfigServiceImpl.class).asEagerSingleton();
        bind(IStatisticsService.class).to(StatisticsServiceImpl.class).asEagerSingleton();
        bind(IGeneService.class).to(GeneServiceImpl.class).asEagerSingleton();
        bind(IOrganismService.class).to(OrganismServiceImpl.class).asEagerSingleton();
        bind(IKingdomService.class).to(KingdomServiceImpl.class).asEagerSingleton();
        bind(IProgressService.class).to(ProgressServiceImpl.class).asEagerSingleton();
        bind(IProgramStatsService.class).to(ProgramStatsServiceImpl.class).asEagerSingleton();
        bind(IZipService.class).to(ZipServiceImpl.class).asEagerSingleton();
    }

    @Provides
    RateLimiter provideRateLimiter() {
        return RateLimiter.create(2);
    }

    @Provides
    CloseableHttpAsyncClient provideHttpAsyncClient() {
        return HttpAsyncClients.createDefault();
    }

    @Provides @Named("HttpExecutor")
    ListeningExecutorService provideHttpListeningExecutorService() {
        return MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(12));
    }

    @Provides
    ListeningExecutorService provideListeningExecutorService() {
        return MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(12));
    }

    @Provides @Named("ProgramStatsExecutor")
    ListeningExecutorService provideProgramStatsExecutor() {
        return  MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
    }

    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new Main());

        MainController mainController = injector.getInstance(MainController.class);
    }
}
