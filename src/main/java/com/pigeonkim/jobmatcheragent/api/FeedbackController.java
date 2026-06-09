package com.pigeonkim.jobmatcheragent.api;

import com.pigeonkim.jobmatcheragent.domain.FeedbackLog;
import com.pigeonkim.jobmatcheragent.feedback.FeedbackService;
import com.pigeonkim.jobmatcheragent.domain.FeedbackType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/{matchResultId}")
    public ResponseEntity<FeedbackLog> saveFeedback(
            @PathVariable Long matchResultId,
            @RequestParam FeedbackType feedbackType,
            @RequestParam(required = false) String memo) {

        FeedbackLog saved = feedbackService.saveFeedback(matchResultId, feedbackType, memo);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<FeedbackLog>> getFeedbacks(
            @RequestParam FeedbackType feedbackType) {

        return ResponseEntity.ok(feedbackService.getFeedbackByType(feedbackType));
    }
}