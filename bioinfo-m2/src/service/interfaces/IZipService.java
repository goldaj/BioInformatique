package service.interfaces;

import java.io.File;
import com.google.common.util.concurrent.ListenableFuture;

public interface IZipService {

	ListenableFuture<Boolean> createGenomeDirectory(File node);
	
}
