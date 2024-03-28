package com.outdecision.Outdecision;
import java.util.*;

public class UserBasedCF {
    private final Map<String, List<Map<String, Integer>>> boardData;  // 게시판 데이터 저장
    private final Map<String, Map<String, Integer>> userData;         // 사용자 데이터 저장
    private final double[][] similarityMatrix;                        // 유사도 행렬 저장

    // 생성자
    public UserBasedCF(Map<String, List<Map<String, Integer>>> boardData, Map<String, Map<String, Integer>> userData){
        this.boardData = boardData;
        this.userData = userData;
        this.similarityMatrix = computeSimilarityMatrix();  // 유사도 행렬 계산
    }

    // 유사도 행렬 계산 메서드
    private double[][] computeSimilarityMatrix(){
        int numUsers = userData.size();  // 사용자 수
        double[][] similarityMatrix = new double[numUsers][numUsers];  // 유사도 행렬 초기화

        int i = 0;
        for(Map<String, Integer> user1Actions : userData.values()) {
            int j = 0;
            for (Map<String, Integer> user2Actions : userData.values()) {
                if (i != j) {
                    similarityMatrix[i][j] = computeSimilarity(user1Actions, user2Actions);  // 두 사용자 간의 유사도 계산
                }
                j++;
            }
            i++;
        }
        return similarityMatrix;
    }

    // 두 사용자 간의 유사도 계산 메서드
    private double computeSimilarity(Map<String, Integer> user1Actions, Map<String, Integer> user2Actions){
        Set<String> user1CategorySet = user1Actions.keySet();  // 사용자 1이 활동한 카테고리 집합
        Set<String> user2CategorySet = user2Actions.keySet();  // 사용자 2가 활동한 카테고리 집합

        Set<String> commonCategories = new HashSet<>(user1CategorySet);  // 공통 카테고리 초기화
        commonCategories.retainAll(user2CategorySet);  // 두 사용자가 모두 활동한 카테고리만 남김
        int numCommonCategories = commonCategories.size();  // 공통 카테고리 수 계산

        if(numCommonCategories == 0){
            return 0;  // 공통 카테고리가 없으면 유사도 0 반환
        }

        // 유사도 계산에 사용될 변수 초기화
        int user1Sum = 0, user2Sum = 0, user1SquaredSum = 0, user2SquaredSum = 0, dotProduct = 0;

        // 공통 카테고리에 대해 유사도 계산
        for (String category : commonCategories) {
            int user1Value = user1Actions.get(category);
            int user2Value = user2Actions.get(category);
            user1Sum += user1Value;
            user2Sum += user2Value;
            user1SquaredSum += (int) Math.pow(user1Value, 2);
            user2SquaredSum += (int) Math.pow(user2Value, 2);
            dotProduct += user1Value * user2Value;
        }
        // 코사인 유사도 계산
        double similarity = dotProduct / (Math.sqrt(user1SquaredSum) * Math.sqrt(user2SquaredSum));
        return similarity;
    }

    // 추천 게시글 예측 메서드
    public Map<String, Double> predictRecommendations(String userId) {
        int userIndex = -1;
        int index = 0;
        // 사용자의 인덱스 찾기
        for (String key : userData.keySet()) {
            if (key.equals(userId)) {
                userIndex = index;
                break;
            }
            index++;
        }

        double[] userSimilarities = similarityMatrix[userIndex];  // 해당 사용자의 다른 사용자들과의 유사도 배열

        // 가중치 합과 유사도 합을 저장할 맵 초기화
        Map<String, Double> weightedSum = new HashMap<>();
        Map<String, Double> similaritySum = new HashMap<>();

        // 다른 모든 사용자에 대해 반복하여 가중치 합과 유사도 합 계산
        for (int otherUserIndex = 0; otherUserIndex < userSimilarities.length; otherUserIndex++) {
            if (userIndex != otherUserIndex && userSimilarities[otherUserIndex] > 0) {
                String otherUserId = (String) userData.keySet().toArray()[otherUserIndex];
                Map<String, Integer> otherUserActions = userData.get(otherUserId);
                for (Map.Entry<String, Integer> entry : otherUserActions.entrySet()) {
                    String category = entry.getKey();
                    int value = entry.getValue();
                    if (!userData.get(userId).containsKey(category) || userData.get(userId).get(category) == 0) {
                        weightedSum.putIfAbsent(category, 0.0);
                        similaritySum.putIfAbsent(category, 0.0);
                        weightedSum.put(category, weightedSum.get(category) + userSimilarities[otherUserIndex] * value);
                        similaritySum.put(category, similaritySum.get(category) + userSimilarities[otherUserIndex]);
                    }
                }
            }
        }

        // 예측된 추천 점수를 계산하여 맵에 저장
        Map<String, Double> recommendations = new HashMap<>();
        for (Map.Entry<String, Double> entry : weightedSum.entrySet()) {
            String category = entry.getKey();
            double weightedSumValue = entry.getValue();
            recommendations.put(category, weightedSumValue / similaritySum.get(category));
        }

        return recommendations;
    }
}