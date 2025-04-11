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

@Configuration
@RequiredArgsConstructor
public class FirstBatch {
    //자동생성
    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;  //chunk가 진행되다 실패했을 때 다시 rollback이나 재시도할 수 있게 도와줌
    //생성한 레포들
    private final BeforeRepository beforeRepository;
    private final AfterRepository afterRepository;

    @Bean
    public Job firstJob() {

        System.out.println("first job");

        return new JobBuilder("firstJob", jobRepository)        //Job이름과 저장할 job레포
                .start(firstStep())         //처음 수행할 step
                /* 순차적으로 수행할 step들
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
