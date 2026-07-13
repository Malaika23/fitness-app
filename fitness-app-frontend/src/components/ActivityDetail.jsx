import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { getActivityDetail, getRecommendationForActivity } from '../services/api';
import { Box, Card, CardContent, Divider, Typography, CircularProgress } from '@mui/material';

const ActivityDetail = () => {
    const { id } = useParams();
    const [activity, setActivity] = useState(null);
    const [recommendation, setRecommendation] = useState(null);
    const [loadingRecommendation, setLoadingRecommendation] = useState(false);
    const [pollingAttempts, setPollingAttempts] = useState(0);

    useEffect(() => {
        const fetchActivityDetail = async () => {
            try {
                const response = await getActivityDetail(id);
                setActivity(response.data);
                if (response.data.recommendation) {
                    setRecommendation({
                        recommendation: response.data.recommendation,
                        improvements: response.data.improvements,
                        suggestions: response.data.suggestions,
                        safety: response.data.safety
                    });
                } else {
                    // Recommendation is not ready yet, start polling
                    setLoadingRecommendation(true);
                }
            } catch (error) {
                console.error(error);
            }
        }

        fetchActivityDetail();
    }, [id]);

    useEffect(() => {
        if (!loadingRecommendation) return;

        const pollInterval = setInterval(async () => {
            if (pollingAttempts >= 10) {
                clearInterval(pollInterval);
                setLoadingRecommendation(false);
                return;
            }

            try {
                const recommendationData = await getRecommendationForActivity(id);
                if (recommendationData) {
                    setRecommendation({
                        recommendation: recommendationData.recommendation,
                        improvements: recommendationData.improvements,
                        suggestions: recommendationData.suggestions,
                        safety: recommendationData.safetyMeasures
                    });
                    setLoadingRecommendation(false);
                    clearInterval(pollInterval);
                }
            } catch (error) {
                // If it returned 404 or failed, increment attempt count and keep trying
                setPollingAttempts(prev => prev + 1);
            }
        }, 2000);

        return () => clearInterval(pollInterval);
    }, [loadingRecommendation, pollingAttempts, id]);

    if (!activity) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh' }}>
                <CircularProgress />
            </Box>
        );
    }

    return (
        <Box sx={{ maxWidth: 800, mx: 'auto', p: 2 }}>
            <Card sx={{ mb: 2, borderRadius: 2, boxShadow: '0 4px 20px rgba(0,0,0,0.08)' }}>
                <CardContent>
                    <Typography variant="h5" gutterBottom sx={{ fontWeight: 600, color: '#2c3e50' }}>Activity Details</Typography>
                    <Typography sx={{ mb: 1, color: '#555' }}>
                        <strong>Type:</strong> {activity.type === "OTHER" && activity.additionalMetrics?.customType 
                            ? `Other (${activity.additionalMetrics.customType})` 
                            : activity.type}
                    </Typography>
                    <Typography sx={{ mb: 1, color: '#555' }}><strong>Duration:</strong> {activity.duration} minutes</Typography>
                    <Typography sx={{ mb: 1, color: '#555' }}><strong>Calories Burned:</strong> {activity.caloriesBurned} kcal</Typography>
                    <Typography sx={{ mb: 1, color: '#555' }}><strong>Date:</strong> {new Date(activity.createdAt).toLocaleString()}</Typography>
                </CardContent>
            </Card>

            {loadingRecommendation && (
                <Card sx={{ mb: 2, borderRadius: 2, boxShadow: '0 4px 20px rgba(0,0,0,0.08)', background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)' }}>
                    <CardContent sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 4 }}>
                        <CircularProgress size={30} sx={{ mb: 2, color: '#3498db' }} />
                        <Typography variant="body1" sx={{ fontWeight: 500, color: '#2c3e50', textAlign: 'center' }}>
                            Generating AI recommendation...
                        </Typography>
                        <Typography variant="body2" sx={{ color: '#7f8c8d', mt: 1, textAlign: 'center' }}>
                            Our Gemini model is analyzing your activity metrics to craft personalized suggestions. This should take a few seconds.
                        </Typography>
                    </CardContent>
                </Card>
            )}

            {!loadingRecommendation && !recommendation && pollingAttempts >= 10 && (
                <Card sx={{ mb: 2, borderRadius: 2, boxShadow: '0 4px 20px rgba(0,0,0,0.08)', borderLeft: '5px solid #f1c40f' }}>
                    <CardContent>
                        <Typography variant="h6" sx={{ color: '#d35400', fontWeight: 600 }}>AI Recommendations Pending</Typography>
                        <Typography variant="body2" sx={{ color: '#7f8c8d', mt: 1 }}>
                            The AI recommendations are taking slightly longer to generate. Please refresh this page in a moment or continue browsing the dashboard.
                        </Typography>
                    </CardContent>
                </Card>
            )}

            {recommendation && (
                <Card sx={{ borderRadius: 2, boxShadow: '0 4px 20px rgba(0,0,0,0.08)', borderLeft: '5px solid #3498db' }}>
                    <CardContent>
                        <Typography variant="h5" gutterBottom sx={{ fontWeight: 600, color: '#2c3e50', display: 'flex', alignItems: 'center', gap: 1 }}>
                            ✨ AI Recommendation
                        </Typography>
                        
                        <Typography variant="h6" sx={{ mt: 2, mb: 1, fontWeight: 500, color: '#34495e' }}>Analysis</Typography>
                        <Typography sx={{ mb: 2, color: '#2c3e50', lineHeight: 1.6 }}>{recommendation.recommendation}</Typography>

                        {recommendation.improvements && recommendation.improvements.length > 0 && (
                            <>
                                <Divider sx={{ my: 2 }} />
                                <Typography variant="h6" sx={{ mb: 1, fontWeight: 500, color: '#34495e' }}>Improvements</Typography>
                                {recommendation.improvements.map((improvement, index) => (
                                    <Typography key={index} sx={{ mb: 1, color: '#555' }}>• {improvement}</Typography>
                                ))}
                            </>
                        )}

                        {recommendation.suggestions && recommendation.suggestions.length > 0 && (
                            <>
                                <Divider sx={{ my: 2 }} />
                                <Typography variant="h6" sx={{ mb: 1, fontWeight: 500, color: '#34495e' }}>Suggestions</Typography>
                                {recommendation.suggestions.map((suggestion, index) => (
                                    <Typography key={index} sx={{ mb: 1, color: '#555' }}>• {suggestion}</Typography>
                                ))}
                            </>
                        )}

                        {recommendation.safety && recommendation.safety.length > 0 && (
                            <>
                                <Divider sx={{ my: 2 }} />
                                <Typography variant="h6" sx={{ mb: 1, fontWeight: 500, color: '#34495e' }}>Safety Guidelines</Typography>
                                {recommendation.safety.map((safetyItem, index) => (
                                    <Typography key={index} sx={{ mb: 1, color: '#555' }}>• {safetyItem}</Typography>
                                ))}
                            </>
                        )}
                    </CardContent>
                </Card>
            )}
        </Box>
    )
}

export default ActivityDetail