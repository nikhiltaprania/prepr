package com.springrag.interview.web;

import com.springrag.interview.domain.enums.DifficultyLevel;
import com.springrag.interview.domain.enums.InterviewType;
import com.springrag.interview.dto.BankQuestionRequest;
import com.springrag.interview.dto.BankQuestionResponse;
import com.springrag.interview.service.QuestionBankService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin question-bank management. The bank seeds interview plans (see InterviewQuestionService), so it
 * is not an isolated feature — curated questions flow into real interviews.
 */
@RestController
@RequestMapping("/api/admin/questions")
@PreAuthorize("hasRole('ADMIN')")
public class QuestionBankController {

    private final QuestionBankService service;

    public QuestionBankController(QuestionBankService service) {
        this.service = service;
    }

    @GetMapping
    public List<BankQuestionResponse> list() {
        return service.list();
    }

    @PostMapping
    public BankQuestionResponse create(@Valid @RequestBody BankQuestionRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public BankQuestionResponse update(@PathVariable UUID id, @Valid @RequestBody BankQuestionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    /** Generate {@code count} AI questions into the bank for a stack/difficulty/category. */
    @PostMapping("/generate")
    public List<BankQuestionResponse> generate(@RequestParam InterviewType type,
                                               @RequestParam DifficultyLevel difficulty,
                                               @RequestParam(required = false) String category,
                                               @RequestParam(defaultValue = "5") int count) {
        return service.generate(type, difficulty, category, count);
    }
}
