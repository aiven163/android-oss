package com.kickstarter.viewmodels;


import android.support.annotation.NonNull;
import android.util.Pair;

import com.kickstarter.KSRobolectricTestCase;
import com.kickstarter.factories.ProjectFactory;
import com.kickstarter.factories.ProjectStatsEnvelopeFactory;
import com.kickstarter.factories.ProjectsEnvelopeFactory;
import com.kickstarter.libs.Environment;
import com.kickstarter.libs.RefTag;
import com.kickstarter.libs.utils.ListUtils;
import com.kickstarter.models.Project;
import com.kickstarter.services.apiresponses.ProjectStatsEnvelope;
import com.kickstarter.services.MockApiClient;
import com.kickstarter.services.apiresponses.ProjectsEnvelope;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.observers.TestSubscriber;

public class CreatorDashboardViewModelTest extends KSRobolectricTestCase {
  private CreatorDashboardViewModel.ViewModel vm;

  private final TestSubscriber<Project> latestProject = new TestSubscriber<>();
  private final TestSubscriber<Pair<Project, ProjectStatsEnvelope>> projectAndStats = new TestSubscriber<>();
  private final TestSubscriber<Pair<Project, RefTag>> startProjectActivity = new TestSubscriber<>();

  protected void setUpEnvironment(final @NonNull Environment environment) {
    this.vm = new CreatorDashboardViewModel.ViewModel(environment);
    this.vm.outputs.latestProject().subscribe(this.latestProject);
    this.vm.outputs.startProjectActivity().subscribe(this.startProjectActivity);
    this.vm.outputs.projectAndStats().subscribe(this.projectAndStats);
  }

  @Test
  public void testStartProjectActivity() {
    final List<Project> projects = Arrays.asList(
      ProjectFactory.project()
    );

    final MockApiClient apiClient = new MockApiClient() {
      @Override public @NonNull
      Observable<ProjectsEnvelope> fetchProjects(final boolean member) {
        return Observable.just(ProjectsEnvelopeFactory.projectsEnvelope(projects));
      }
    };
    setUpEnvironment(environment().toBuilder().apiClient(apiClient).build());
    this.vm.inputs.projectViewClicked();
    this.startProjectActivity.assertValues(Pair.create(ListUtils.first(projects), RefTag.dashboard()));
  }

  @Test
  public void testLatestProjectAndStats() {
    final List<Project> projects = Arrays.asList(
      ProjectFactory.project()
    );

    final ProjectStatsEnvelope ProjectStatsEnvelope = ProjectStatsEnvelopeFactory.ProjectStatsEnvelope();
    final MockApiClient apiClient = new MockApiClient() {
      @Override public @NonNull Observable<ProjectsEnvelope> fetchProjects(final boolean member) {
        return Observable.just(ProjectsEnvelopeFactory.projectsEnvelope(projects));
      }
      @Override public @NonNull
      Observable<ProjectStatsEnvelope> fetchProjectStats(final Project project) {
        return Observable.just(ProjectStatsEnvelope);
      }
    };

    setUpEnvironment(environment().toBuilder().apiClient(apiClient).build());
    this.latestProject.assertValues(ListUtils.first(projects));
    final Pair<Project, ProjectStatsEnvelope> outputPair = Pair.create(ListUtils.first(projects), ProjectStatsEnvelope);
    this.projectAndStats.assertValues(outputPair);
  }
}
