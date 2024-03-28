package com.outdecision.Outdecision;
import java.sql.*;
import java.util.*;

public class MySQLConnector {
    public static void main(String[] args) {
        Connection connection = null;
        Statement statement = null;
        try {
            // MySQL 데이터베이스에 연결
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/board", "root", "0000");
            statement = connection.createStatement();

            // board_data 테이블에서 데이터 가져오기
            ResultSet boardDataResult = statement.executeQuery("SELECT * FROM board_data");
            Map<String, List<Map<String, Integer>>> boardData = new HashMap<>();
            while (boardDataResult.next()) {
                String category = boardDataResult.getString("category");
                int postNumber = boardDataResult.getInt("post_number");
                int recommendCount = boardDataResult.getInt("recommend_count");
                int viewCount = boardDataResult.getInt("view_count");
                boardData.computeIfAbsent(category, k -> new ArrayList<>()).add(
                        new HashMap<>() {{
                            put("게시글 번호", postNumber);
                            put("추천 수", recommendCount);
                            put("조회수", viewCount);
                        }}
                );
            }

            // user_data 테이블에서 데이터 가져오기
            ResultSet userDataResult = statement.executeQuery("SELECT * FROM user_data");
            Map<String, Map<String, Integer>> userData = new HashMap<>();
            while (userDataResult.next()) {
                String userId = userDataResult.getString("user_id");
                String category = userDataResult.getString("category");
                int postNumber = userDataResult.getInt("post_number");
                userData.computeIfAbsent(userId, k -> new HashMap<>()).put(category, postNumber);
            }

            // 사용자 기반 협업 필터링 모델 초기화
            UserBasedCF userBasedCF = new UserBasedCF(boardData, userData);
            String userId = "user1"; // 사용자 ID 설정
            Map<String, Double> recommendations = userBasedCF.predictRecommendations(userId);

            // 해당 사용자의 추천 게시글 예측 점수 출력
            System.out.println(userId + "의 추천 게시글 예측 점수:");
            for (Map.Entry<String, Double> entry : recommendations.entrySet()) {
                System.out.println("카테고리: " + entry.getKey() + ", 예측 점수: " + String.format("%.2f", entry.getValue()));
            }

            // 해당 사용자의 맞춤 추천 게시글 출력
            System.out.println("\n" + userId + "의 맞춤 추천 게시글:");
            // 맞춤 추천 게시글 출력
            List<String> recommendationBoardNames = new ArrayList<>(recommendations.keySet());
            recommendationBoardNames = recommendationBoardNames.subList(0, Math.min(5, recommendationBoardNames.size())); // 상위 5개 게시판만 선택
            List<Integer> recommendationPostNumbers = new ArrayList<>();
            Set<Integer> seenPostNumbers = new HashSet<>();  // 중복 확인을 위한 Set

            for (String category : recommendationBoardNames) {
                List<Map<String, Integer>> posts = boardData.get(category);
                if (posts != null) {
                    posts.sort((post1, post2) -> {
                        int score1 = post1.get("추천 수")*10 + post1.get("조회수");
                        int score2 = post2.get("추천 수")*10 + post2.get("조회수");
                        return Integer.compare(score2, score1); // 내림차순 정렬
                    });

                    for (Map<String, Integer> post : posts) {
                        int postNumber = post.get("게시글 번호");
                        if (!seenPostNumbers.contains(postNumber)) {
                            recommendationPostNumbers.add(postNumber);
                            seenPostNumbers.add(postNumber);
                        }
                        if (recommendationPostNumbers.size() == 5) {
                            break;
                        }
                    }
                }
                if (recommendationPostNumbers.size() == 5) {
                    break;
                }
            }

            System.out.println(recommendationPostNumbers); // 수정된 맞춤 추천 게시글 출력

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
