package com.prsnl.app;

import com.prsnl.document.repository.NotebookRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<NotebookRepository> notebookRepositoryProvider;

  public MainActivity_MembersInjector(Provider<NotebookRepository> notebookRepositoryProvider) {
    this.notebookRepositoryProvider = notebookRepositoryProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<NotebookRepository> notebookRepositoryProvider) {
    return new MainActivity_MembersInjector(notebookRepositoryProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectNotebookRepository(instance, notebookRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.prsnl.app.MainActivity.notebookRepository")
  public static void injectNotebookRepository(MainActivity instance,
      NotebookRepository notebookRepository) {
    instance.notebookRepository = notebookRepository;
  }
}
