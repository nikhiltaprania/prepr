package com.springrag.interview.service.impl;

import com.springrag.common.exception.ResourceNotFoundException;
import com.springrag.interview.domain.BankQuestion;
import com.springrag.interview.domain.enums.DifficultyLevel;
import com.springrag.interview.domain.enums.InterviewType;
import com.springrag.interview.dto.BankQuestionRequest;
import com.springrag.interview.dto.BankQuestionResponse;
import com.springrag.interview.repository.BankQuestionRepository;
import com.springrag.interview.service.QuestionBankService;
import com.springrag.interview.service.QuestionGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class QuestionBankServiceImpl implements QuestionBankService {

    private static final Logger log = LoggerFactory.getLogger(QuestionBankServiceImpl.class);

    private final BankQuestionRepository repository;
    private final QuestionGenerator questionGenerator;

    public QuestionBankServiceImpl(BankQuestionRepository repository, QuestionGenerator questionGenerator) {
        this.repository = repository;
        this.questionGenerator = questionGenerator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankQuestionResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public BankQuestionResponse create(BankQuestionRequest request) {
        BankQuestion q = new BankQuestion();
        apply(q, request);
        q.setAiGenerated(false);
        return toResponse(repository.save(q));
    }

    @Override
    @Transactional
    public BankQuestionResponse update(UUID id, BankQuestionRequest request) {
        BankQuestion q = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + id));
        apply(q, request);
        return toResponse(repository.save(q));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) throw new ResourceNotFoundException("Question not found: " + id);
        repository.deleteById(id);
    }

    @Override
    public List<BankQuestionResponse> generate(InterviewType type, DifficultyLevel difficulty, String category, int count) {
        List<String> existing = new ArrayList<>(
                repository.findByTypeAndDifficulty(type, difficulty).stream().map(BankQuestion::getQuestion).toList());
        List<BankQuestionResponse> created = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String text = questionGenerator.generateOne(type, difficulty, existing, List.of());
            if (text.isBlank()) continue;
            BankQuestion q = new BankQuestion();
            q.setType(type);
            q.setDifficulty(difficulty);
            q.setCategory(category);
            q.setQuestion(text);
            q.setAiGenerated(true);
            created.add(toResponse(repository.save(q)));
            existing.add(text);
        }
        log.info("Generated {} bank questions. type={}, difficulty={}", created.size(), type, difficulty);
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankQuestion> draw(InterviewType type, DifficultyLevel difficulty, int limit, List<String> excludeTexts) {
        if (limit <= 0) return List.of();
        List<BankQuestion> pool = new ArrayList<>(repository.findByTypeAndDifficulty(type, difficulty));
        if (excludeTexts != null && !excludeTexts.isEmpty()) {
            pool.removeIf(q -> excludeTexts.stream().anyMatch(t -> t.equalsIgnoreCase(q.getQuestion())));
        }
        Collections.shuffle(pool);
        return pool.stream().limit(limit).toList();
    }

    private void apply(BankQuestion q, BankQuestionRequest r) {
        q.setType(r.getType());
        q.setDifficulty(r.getDifficulty());
        q.setCategory(r.getCategory());
        q.setQuestion(r.getQuestion());
    }

    private BankQuestionResponse toResponse(BankQuestion q) {
        BankQuestionResponse r = new BankQuestionResponse();
        r.setId(q.getId());
        r.setType(q.getType());
        r.setDifficulty(q.getDifficulty());
        r.setCategory(q.getCategory());
        r.setQuestion(q.getQuestion());
        r.setAiGenerated(q.isAiGenerated());
        r.setCreatedAt(q.getCreatedAt());
        return r;
    }
}
