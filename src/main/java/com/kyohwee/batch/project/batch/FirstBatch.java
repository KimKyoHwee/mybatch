package com.kyohwee.batch.project.batch;

import com.kyohwee.batch.project.entity.AfterEntity;
import com.kyohwee.batch.project.entity.BeforeEntity;
import com.kyohwee.batch.project.repository.AfterRepository;
import com.kyohwee.batch.project.repository.BeforeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

//beforeRepository의 모든 데이터를 afterRepository로 복사하는 배치
// https://github.com/spring-projects/spring-batch/tree/main/spring-batch-samples/src/main/java/org/springframework/batch/samples/mongodb   공식참고 코드들 (JDBC, Mongo ...)
@Configuration
@RequiredArgsConstructor
public class FirstBatch {
    //자동생성
    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;  //chunk가 진행되다 실패했을 때 다시 rollback이나 재시도할 수 있게 도와줌
    //생성한 레포들
    private final BeforeRepository beforeRepository;
    private final AfterRepository afterRepository;

    /* TODO: STEP 전이나 후에 특정 작업을 넣고싶을 때
    @Bean
public StepExecutionListener stepExecutionListener() {

    return new StepExecutionListener() {
        @Override
        public void beforeStep(StepExecution stepExecution) {
            StepExecutionListener.super.beforeStep(stepExecution);
        }

        @Override
        public ExitStatus afterStep(StepExecution stepExecution) {
            return StepExecutionListener.super.afterStep(stepExecution);
        }
    };
}

@Bean
public Step sixthStep() {

    return new StepBuilder("sixthStep", jobRepository)
            .<BeforeEntity, AfterEntity> chunk(10, platformTransactionManager)
            .reader(beforeSixthReader())
            .processor(middleSixthProcessor())
            .writer(afterSixthWriter())
            .listener(stepExecutionListener())
            .build();
}
     */

    /* TODO: JOB 실행 전이나 후에 작업을 넣고 싶을 때
        @Bean
    public JobExecutionListener jobExecutionListener() {

        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                JobExecutionListener.super.beforeJob(jobExecution);
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                JobExecutionListener.super.afterJob(jobExecution);
            }
        };
    }

    @Bean
    public Job sixthBatch() {

        return new JobBuilder("sixthBatch", jobRepository)
                .start(sixthStep())
                .listener(jobExecutionListener())
                .build();
    }
     */

    @Bean
    public Job firstJob() {

        System.out.println("first job");

        /* TODO: Job 조건절
				.start(stepA)
				.on("*").to(stepB) //무슨 결과가 나와도 stepB실행
				.from(stepA).on("FAILED").to(stepC)  //stepA가 실패하면 stepC실행
				.from(stepA).on("COMPLETED").to(stepD)
				.end()
				.build();
}
         */
        return new JobBuilder("firstJob", jobRepository)        //Job이름과 저장할 job레포
                .start(firstStep())         //처음 수행할 step
                /* TODO: 순차적으로 수행할 step들
                .next()
                .next()
                 */
                .build();
    }

    @Bean
    public Step firstStep() {

        System.out.println("first step");

        return new StepBuilder("firstStep", jobRepository)   //step 이름, 트래킹할 job 레포
                .<BeforeEntity, AfterEntity> chunk(10, platformTransactionManager)  //<READ할 데이터 타입, WRITE할 데이터 타입>, 10개씩 데이터를 끊어서 읽어들이기 (chunk가 단위)
                .reader(beforeReader())  //읽는 메소드 자리
                .processor(middleProcessor())  //데이터 처리 메소드 자리
                .writer(afterWriter())
                /*  TODO: 오류 발생시 스킵
                .faultTolerant()
            .skip(Exception.class)  //스킵할 오류
            .noSkip(FileNotFoundException.class)  //스킵하지 않을 오류
            .noSkip(IOException.class)
            .skipLimit(10)  //최대 몇번 스킵
                 */
                /* TODO: 오류 발생시 재시도
                .faultTolerant()
            .retryLimit(3)
            .retry(SQLException.class)
            .retry(IOException.class)
            .noRetry(FileNotFoundException.class)
                 */
                .build();
    }

    @Bean
    public RepositoryItemReader<BeforeEntity> beforeReader() {

        return new RepositoryItemReaderBuilder<BeforeEntity>()
                .name("beforeReader")     //만든 Reader 이름 정의
                .pageSize(10)             //10개씩 읽겠다 (10개 단위로 읽고 처리하고 쓰겠다)
                .methodName("findAll")    //findById 등 여러개 설정 가능
                .repository(beforeRepository)  //"findAll" 을 수행할 레포
                .sorts(Map.of("id", Sort.Direction.ASC))  //데이터의 순서를 오름차순으로 세팅
                .build();
    }

    //읽어온 데이터 가공
    @Bean
    public ItemProcessor<BeforeEntity, AfterEntity> middleProcessor() {

        return new ItemProcessor<BeforeEntity, AfterEntity>() {

            @Override
            public AfterEntity process(BeforeEntity item) throws Exception {  //item은 위의 Reader로부터 땡겨온 데이터 단위

                //데이터 가공
                AfterEntity afterEntity = new AfterEntity();
                afterEntity.setUsername(item.getUsername());


                return afterEntity;
            }
        };
    }

    //데이터 쓰기 step
    @Bean
    public RepositoryItemWriter<AfterEntity> afterWriter() {

        return new RepositoryItemWriterBuilder<AfterEntity>()
                .repository(afterRepository)
                .methodName("save")  //위의 레포에 "save" 작업 수행
                .build();
    }
}
